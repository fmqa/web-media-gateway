(function (url, selector, mediatype) {
    if (!url) {
        console.error('No stream URL given');
        return;
    }

    const mediaElement = document.querySelector(selector);
    if (!mediaElement) {
        console.error('Could not find element with selector: ' + selector);
        return;
    }

    if (!MediaSource.isTypeSupported(mediatype)) {
        console.error('MSE not supported by this browser for media type: ' + mediatype);
        return;
    }

    if (mediaElement.src) {
        URL.revokeObjectURL(mediaElement.src);
    }

    const mediaSource = new MediaSource();
    mediaElement.src = URL.createObjectURL(mediaSource);

    let sourceBuffer = null;
    let currentAbortController = null;
    let isDurationSet = false;
    let currentSeekId = 0; // Incremental token to invalidate stale async fetches
    let isProgrammaticSeek = false; // Prevents gap-snapping from re-triggering seeking listener

    // Live / Growing stream state
    let knownDuration = 0;
    let isFetchingSegment = false;

    function getNumericDatasetAttr(element, key, defaultValue) {
        const val = element.dataset[key];
        if (val === undefined || val === null || val.trim() === '') {
            return defaultValue;
        }
        const parsed = Number(val);
        return Number.isNaN(parsed) ? defaultValue : parsed;
    }

    // Buffer management thresholds (in seconds)
    const maxForwardBuffer = getNumericDatasetAttr(mediaElement, 'maxForwardBuffer', 40);
    const backwardBufferLimit = getNumericDatasetAttr(mediaElement, 'backwardBufferLimit', 15);

    mediaSource.addEventListener('sourceopen', onSourceOpen);

    function onSourceOpen() {
        mediaSource.removeEventListener('sourceopen', onSourceOpen);
        // Create single SourceBuffer instance for the lifetime of MediaSource
        sourceBuffer = mediaSource.addSourceBuffer(mediatype);
        executeSeek(0);
    }

    /**
     * Issues a HEAD request to inspect stream duration and update MediaSource state.
     */
    async function checkStreamUpdate() {
        if (mediaSource.readyState !== 'open') return;

        try {
            const response = await fetch(url, { method: 'HEAD', cache: 'no-cache' });
            if (!response.ok) return;

            const durationHeader = response.headers.get('Content-Duration');
            if (durationHeader) {
                const total = parseFloat(durationHeader);
                if (!isNaN(total) && isFinite(total) && total > knownDuration) {
                    knownDuration = total;
                    if (mediaSource.readyState === 'open') {
                        mediaSource.duration = total;
                    }
                    resumeFetchIfNeeded();
                }
            }
        } catch (e) {
            // Ignore transient network errors
        }
    }

    /**
     * Expose controls to the embedder via mediaElement method & event listener
     */
    mediaElement.addEventListener('checkstreamupdate', checkStreamUpdate);

    /**
     * Resumes fetching if more duration is available than what is currently buffered.
     */
    function resumeFetchIfNeeded() {
        if (isFetchingSegment || mediaSource.readyState !== 'open') return;

        let maxBufferedEnd = 0;
        if (sourceBuffer && sourceBuffer.buffered.length > 0) {
            maxBufferedEnd = sourceBuffer.buffered.end(sourceBuffer.buffered.length - 1);
        }

        // Resume fetch if known duration extends beyond buffer
        if (maxBufferedEnd < knownDuration - 0.5) {
            if (currentAbortController) currentAbortController.abort();
            currentAbortController = new AbortController();

            fetchAndAppendSegment(maxBufferedEnd, currentSeekId, currentAbortController.signal);
        }
    }

    /**
     * Safely waits for any ongoing update on the SourceBuffer to finish.
     */
    function waitForUpdateEnd() {
        return new Promise((resolve) => {
            if (!sourceBuffer || !sourceBuffer.updating) {
                resolve();
            } else {
                const onUpdateEnd = () => {
                    sourceBuffer.removeEventListener('updateend', onUpdateEnd);
                    sourceBuffer.removeEventListener('error', onUpdateEnd);
                    resolve();
                };
                sourceBuffer.addEventListener('updateend', onUpdateEnd);
                sourceBuffer.addEventListener('error', onUpdateEnd);
            }
        });
    }

    /**
     * Finds the buffered time range containing or closest to the given time.
     */
    function getBufferedRangeForTime(time) {
        if (!sourceBuffer) return null;
        const buffered = sourceBuffer.buffered;
        for (let i = 0; i < buffered.length; i++) {
            if (time >= buffered.start(i) - 0.5 && time <= buffered.end(i) + 0.5) {
                return { start: buffered.start(i), end: buffered.end(i) };
            }
        }
        if (buffered.length > 0) {
            return { start: buffered.start(buffered.length - 1), end: buffered.end(buffered.length - 1) };
        }
        return null;
    }

    /**
     * Calculates the buffered duration ahead of a given timestamp.
     */
    function getForwardBuffer(time) {
        if (!sourceBuffer || sourceBuffer.buffered.length === 0) return 0;
        const buffered = sourceBuffer.buffered;
        for (let i = 0; i < buffered.length; i++) {
            if (time >= buffered.start(i) - 0.5 && time <= buffered.end(i) + 0.5) {
                return Math.max(0, buffered.end(i) - time);
            }
        }
        return 0;
    }

    /**
     * Checks if a target timestamp is already available in memory.
     */
    function isTimeBuffered(time) {
        if (!sourceBuffer) return false;
        const buffered = sourceBuffer.buffered;
        for (let i = 0; i < buffered.length; i++) {
            const start = buffered.start(i);
            const end = buffered.end(i);
            if (time >= start && time < end - 0.05) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles seek execution, buffer eviction, and timestamp offset synchronization.
     */
    async function executeSeek(targetTime) {
        currentSeekId++;
        const seekId = currentSeekId;

        // If there is an ongoing fetch operation, abort it
        if (currentAbortController) {
            currentAbortController.abort();
        }
        currentAbortController = new AbortController();
        const signal = currentAbortController.signal;

        // Wait for buffer pending buffer operations to conclude
        await waitForUpdateEnd();
        if (seekId !== currentSeekId) return;

        // Re-open MediaSource if it transitioned to 'ended' state
        if (mediaSource.readyState === 'ended') {
            try {
                sourceBuffer.appendBuffer(new Uint8Array(0));
                await waitForUpdateEnd();
            } catch (e) {}
        }

        if (seekId !== currentSeekId || mediaSource.readyState !== 'open') return;

        // Reset internal segment parser
        try {
            sourceBuffer.abort();
        } catch (e) {}

        // Clear old buffered ranges to avoid timestamp conflicts
        if (sourceBuffer.buffered.length > 0) {
            try {
                const duration = (isFinite(mediaSource.duration) && mediaSource.duration > 0)
                    ? mediaSource.duration
                    : Infinity;
                sourceBuffer.remove(0, duration);
                await waitForUpdateEnd();
            } catch (e) {}
        }

        if (seekId !== currentSeekId || mediaSource.readyState !== 'open') return;

        // Assign timestampOffset to align incoming stream frames with global time
        try {
            sourceBuffer.timestampOffset = targetTime;
        } catch (e) {}

        // Fetch and feed stream
        await fetchAndAppendSegment(targetTime, seekId, signal);
    }

    /**
     * Fetches stream segment starting at startTime and streams chunks into SourceBuffer.
     */
    async function fetchAndAppendSegment(startTime, seekId, signal) {
        isFetchingSegment = true;

        try {
            const requestUrl = new URL(url);
            requestUrl.searchParams.set('start', `PT${startTime}S`);
            const response = await fetch(requestUrl.toString(), { signal });

            if (seekId !== currentSeekId || signal.aborted) return;
            if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);

            // Extract total duration header
            const durationHeader = response.headers.get('Content-Duration');
            if (durationHeader && !isDurationSet) {
                const total = parseFloat(durationHeader);
                if (!isNaN(total) && isFinite(total) && total > 0 && mediaSource.readyState === 'open') {
                    mediaSource.duration = total;
                    isDurationSet = true;
                }
            }

            const reader = response.body.getReader();
            let isFirstChunk = true;

            while (true) {
                // Throttle fetch loop if forward buffer exceeds limit (prevents QuotaExceededError)
                while (getForwardBuffer(mediaElement.currentTime) >= maxForwardBuffer) {
                    await new Promise((resolve) => setTimeout(resolve, 250));
                    if (seekId !== currentSeekId || signal.aborted || mediaSource.readyState !== 'open') break;
                }

                if (seekId !== currentSeekId || signal.aborted || mediaSource.readyState !== 'open') break;

                const { done, value } = await reader.read();

                if (seekId !== currentSeekId || signal.aborted) {
                    try { reader.cancel(); } catch (e) {}
                    break;
                }

                if (done) {
                    await waitForUpdateEnd();
                    if (seekId === currentSeekId && mediaSource.readyState === 'open' && !sourceBuffer.updating) {
                        try {
                            mediaSource.endOfStream();
                        } catch (e) {}
                    }
                    break;
                }

                await waitForUpdateEnd();
                if (seekId !== currentSeekId || signal.aborted || mediaSource.readyState !== 'open') break;

                // Evict old played buffer behind current playhead
                if (sourceBuffer.buffered.length > 0) {
                    const removeEnd = mediaElement.currentTime - backwardBufferLimit;
                    if (removeEnd > sourceBuffer.buffered.start(0) + 1) {
                        try {
                            sourceBuffer.remove(0, removeEnd);
                            await waitForUpdateEnd();
                        } catch (e) {}
                    }
                }

                if (seekId !== currentSeekId || signal.aborted || mediaSource.readyState !== 'open') break;

                // Append buffer chunk with QuotaExceededError handling
                try {
                    sourceBuffer.appendBuffer(value);
                    await waitForUpdateEnd();
                } catch (appendErr) {
                    if (seekId !== currentSeekId || signal.aborted) break;

                    const isQuotaError = appendErr.name === 'QuotaExceededError' ||
                                         appendErr.code === 22 ||
                                         (appendErr.message && appendErr.message.toLowerCase().includes('buffer'));

                    if (isQuotaError) {
                        const clearEnd = Math.max(0, mediaElement.currentTime - 2);
                        if (sourceBuffer.buffered.length > 0 && clearEnd > sourceBuffer.buffered.start(0)) {
                            try {
                                sourceBuffer.remove(0, clearEnd);
                                await waitForUpdateEnd();
                                if (seekId === currentSeekId && mediaSource.readyState === 'open' && !sourceBuffer.updating) {
                                    sourceBuffer.appendBuffer(value);
                                    await waitForUpdateEnd();
                                }
                            } catch (e) {
                                throw appendErr;
                            }
                        } else {
                            throw appendErr;
                        }
                    } else {
                        throw appendErr;
                    }
                }

                if (seekId !== currentSeekId || signal.aborted) break;

                // GAP SNAPPING: Bridges frame-alignment micro-gaps
                if (isFirstChunk) {
                    isFirstChunk = false;
                    const range = getBufferedRangeForTime(startTime);
                    if (range) {
                        if (mediaElement.currentTime < range.start || mediaElement.currentTime >= range.end) {
                            isProgrammaticSeek = true;
                            mediaElement.currentTime = range.start + 0.01;
                        }
                    }
                }
            }
        } catch (err) {
            if (err.name === 'AbortError' || signal.aborted || seekId !== currentSeekId) {
                return; // Expected cancellation during seeking
            }
            console.error('Stream playback error:', err);
        } finally {
            isFetchingSegment = false;
        }
    }

    // Handle seeking via media element controls
    mediaElement.addEventListener('seeking', () => {
        if (isProgrammaticSeek) {
            isProgrammaticSeek = false;
            return;
        }

        const targetTime = mediaElement.currentTime;

        // Skip fetch if seeking within already buffered ranges
        if (isTimeBuffered(targetTime)) {
            return;
        }

        executeSeek(targetTime);
    });
})(/*[[${streamUrl}]]*/ '', /*[[${elementSelector}]]*/ 'audio', /*[[${mediatype}]]*/ '');