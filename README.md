# Web Media Gateway

```
                                       🗲 FFmpeg
                                           |              ┌─────────────┐
┌──────┐ GET /audio.mp4a?source=… ┌─────────────────┐ GET │ FILE SERVER │
│CLIENT│ ───────────────────────► │WEB MEDIA GATEWAY│ ──► │ [WAV] [L16] │
└──────┘                          └─────────────────┘     │ [OGG] [MP2] │
                                                          └─────────────┘
```

Stateless gateway/proxy for on-the-fly transcoding of media resources to web-compatible formats, playable via MSE.

This gateway is primarily designed for VoIP/telephony applications, but remains usable for other media streaming applications.

Usually, you would place this gateway application behind another high-level gateway that performs resource routing.

## Requirements

* FFmpeg (Working `ffmpeg` and `ffprobe` binaries in `$PATH`)
* JDK27+

## Technical Details / Rationale

For ad-hoc VoIP/Streaming applications, pre-encoding AV media to a persistent storage for later provision as HLS/DASH might not be possible due to space constraints. In this case, it may be preferable to trade the CPU overhead of repeated online encoding for savings in storage space.

This gateway provides a solution in the form of a transcoding proxy that sits between the original (lossless/uncompressed) media or streams and the client. Media is transcoded via FFmpeg on the fly to a web-compatible format.

In order to ensure that audio controls via HTML5 media `<audio>` / `<video>` including navigation remain functional, the gateway provides a [CoD](https://en.wikipedia.org/wiki/Code_on_demand) payload that hooks into a selected media element via MSE, connecting it to the streaming API.

Unlike regular HTML5 media streaming (which relies on byte-range navigation), the MSE adapter provided by the gateway relies on time-based navigation via a `?start=…` parameter, similar to older HTTP pseudo-streaming solutions.

## Minimal Example / Usage

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <title>Media Gateway Demo</title>
</head>
<body>
  <audio id="audioPlayer" controls></audio>
  <script src="/audio.mp4.js?selector=%23audioPlayer&source=http%3A%2F%2F127.0.0.1%3A8000%2Fmyaudio.wav">
</script>
</body>
</html>
```

## API
### Capabilities
#### Proxy
All subsequently listed APIs support forwarding headers (including the _X-Forwarded-Path_ header).

#### HTTP HEAD Support
All _GET_ API endpoints also support the HTTP _HEAD_ verb.

#### Headers
All streaming API responses include a `Content-Duration` header indicating the duration of the media in seconds, incl. subsecond fractions.

### Audio
#### HTTP Pseudo-Streaming
```
GET /audio.wav?source=…[&start=<iso-8601-duration>]
GET /audio.mp3?source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
GET /audio.mp4a?source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
```
```
GET /audio
Accept: audio/x-wav | audio/mpeg | audio/mp4
```
Stream the resource referred to by _source_ in the given format, starting from the given position _start_ (if provided).

The source resource is transcoded on-the-fly. `Content-Duration` is added to the response to indicate the duration of the resource in seconds.

All responses use chunked encoding.

The optional parameter _q_ refers to a quality scale, where 0=best, 9=worst. This is used for VBR encoding.

The optional parameter _b_ specifies an explicit bit rate e.g. 320k, 320000, 120Ki, etc.- this parameter takes priority over _q_, if both are provided.
#### Client support
```
GET /audio.mp4a.js?selector=…&source=…[&q=<quality>&b=<bitrate>]
GET /audio.mp4a?selector=…&source=…[&q=<quality>&b=<bitrate>]
GET /audio.js?selector=…&source=…[&q=<quality>&b=<bitrate>]
GET /audio?selector=…&source=…[&q=<quality>&b=<bitrate>]
```
Responds with a JavaScript payload that configures the `<audio>` or `<video>` DOM element matching _selector_, binding it to the given _source_ via MSE.

MSE support is required. This uses `audio/mp4; codecs="mp4a.40.2"` as the preferred transmission format.

This API is suitable for usage within a `<script>` tag, e.g. `<script src="/video.mp4.js?selector=%23myAudioElementId&source=…"></script>`.

### Video
#### HTTP Pseudo-Streaming
```
GET /video.mp4?source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>&preset=<preset>]
```
```
GET /video?source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>&preset=<preset>]
Accept: video/mp4
```
Stream the resource referred to by _source_ as web-compatible MP4 video (avc1.42E01E, mp4a.40.2), starting from the given position _start_ (if provided).

`Content-Duration` is added to the response to indicate the duration of the resource in seconds.

The parameters _q_ and _b_ have the same meaning and semantics as specified in the audio API.

The optional parameter _preset_ specifies the video encoder preset, which may be one of _ULTRAFAST, SUPERFAST, VERYFAST, FASTER, FAST, MEDIUM, SLOW, SLOWER, VERYSLOW_.

#### Client Support
```
GET /video.mp4.js?selector=…&source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>&preset=<preset>]
GET /video.mp4?selector=…&source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>&preset=<preset>]
GET /video?selector=…&source=…[&start=<iso-8601-duration>&q=<quality>&b=<bitrate>&preset=<preset>]
```

Responds with a JavaScript payload that configures the `<video>` or `<video>` DOM element matching _selector_, binding it to the given _source_ via MSE.

MSE support is required. This uses `video/mp4; codecs="avc1.42E01E, mp4a.40.2"` as the preferred transmission format.

### Mixing
#### HTTP Pseudo-Streaming
```
GET /stereo.wav?source=…[&source=…&source=………&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
GET /stereo.mp3?source=…[&source=…&source=………&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
GET /stereo.mp4a?source=…[&source=…&source=………&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
```
```
GET /stereo?source=…[&source=…&source=………&start=<iso-8601-duration>&q=<quality>&b=<bitrate>]
Accept: audio/x-wav | audio/mpeg | audio/mp4
```
Mixes the given _1..n_ audio streams, performing an on-the-fly stereo mix into the desired format.

_start_, _q_, and _b_ have the same semantics as specified in the audio/video APIs.

`Content-Duration` is added to the response to indicate the duration of the resource in seconds and is equal to the duration of the longest input source.

The mixing scheme may be specified via fragment parameters attached to each _source_ URI:

* `#gain=<number>` Gain (volume) adjustment
* `#balance=0..1` L-R stereo balance
* `#delay=<seconds>` Initial delay in seconds
* `#mask=<start>-<end>`|`#mask=<start>-`|`#mask=-<end>`|`#mask=-` Mask/silence the given range in seconds (may be specified multiple times e.g. `#mask=0-5&mask=10-15`)

Note that this always performs a stereo mix, regardless of the number of input streams.

#### Client Support
```
GET /stereo.mp4a.js?selector=…&source=…[&source=…&source=………&q=<quality>&b=<bitrate>]
GET /stereo.mp4a?selector=…&source=…[&source=…&source=………&q=<quality>&b=<bitrate>]
GET /stereo?selector=…&source=…[&source=…&source=………&q=<quality>&b=<bitrate>]
```
Responds with a JavaScript payload that configures the `<audio>` or `<video>` DOM element matching _selector_, binding it to the given stereo mix of the given _source_  resources via MSE.

MSE support is required. This uses `audio/mp4; codecs="mp4a.40.2"` as the preferred transmission format.

All parameters have the same semantics specified in the corresponding streaming API. _source_ URIs may contain the same fragment parameters as specified in the last section to configure the mixing scheme.

### Jitter-Buffered Streaming
```
GET /jitter.l16?source=…[&start=…&f=…&target=…&max=…]
GET /jitter?source=…[&start=…&f=…&target=…&max=…]
```
Helper API for jitter-buffered audio/l16 (RFC2586) streaming, useful for select VoIP applications. This API responds with a continuous non-blocking byte stream, with silence insertion being performed in case the source resource does not provide data in time. 

_source_ must refer to a RFC2586-compatible audio resource.

_start (optional)_ is an ISO8601 duration specifier indicating the starting position.

_f (optional)_ represents the length (in milliseconds) of a single buffering window (20 by default).

_target (optional)_ represents the targeted buffer delay in milliseconds (60 by default).

_max (optional)_ represents the maximum buffer delay in milliseconds (200 by default).

## License

GPLv3.
