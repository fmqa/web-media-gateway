package com.github.fmqa.spu.media.temporal;

import com.github.fmqa.spu.media.ffmpeg.FFInputable;

import java.util.Arrays;

/**
 * Provides predefined strategies for estimating the duration of media resources.
 */
public class Estimators {
    private Estimators() { }

    /**
     * Combines multiple estimation strategies.
     * <p></p>
     * The resulting composite estimator tries the given estimators sequentially, with the estimated duration being the
     * first non-null one.
     * @param estimators estimation strategies
     * @return an estimator combining the given estimation strategies
     */
    public static Estimator combine(Iterable<? extends Estimator> estimators) {
        return new CombinedEstimator(estimators);
    }

    /**
     * Combines multiple estimation strategies.
     * <p></p>
     * The resulting composite estimator tries the given estimators sequentially, with the estimated duration being the
     * first non-null one.
     * @param estimators estimation strategies
     * @return an estimator combining the given estimation strategies
     */
    public static Estimator combine(Estimator... estimators) {
        return combine(Arrays.asList(estimators));
    }

    /**
     * Decorates an {@link FFInputable} with the given estimator, which will be used as a fallback in case
     * {@link FFInputable#duration()} returns {@code null}.
     * @param input the input to wrap
     * @param estimator the estimator to use a fallback for duration estimator
     * @return a media object which may fall back onto the given duration estimator
     */
    public static FFInputable wrap(FFInputable input, Estimator estimator) {
        return new EstimatedFFInput(input, estimator);
    }
}
