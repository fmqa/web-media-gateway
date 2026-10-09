package com.github.fmqa.spu.media.temporal;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

record CombinedEstimator(Iterable<? extends Estimator> estimators) implements Estimator {
    @Override
    public Duration duration(URI uri) throws IOException, InterruptedException {
        for (final Estimator estim : estimators) {
            final Duration duration = estim.duration(uri);
            if (duration != null) {
                return duration;
            }
        }
        return null;
    }
}
