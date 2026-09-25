package com.github.fmqa.spu.media;

/**
 * Predefined connector implementations.
 */
public enum Connectors implements Connector {
    /**
     * Throws {@link ConnectionRejectedException} on any connection attempt.
     * Used to assert whether a resource is a proper reifiable resource with a URI, as opposed
     * to a virtual resource that requires a piped connection to FFmpeg.
     */
    REJECT {
        @Override
        public String connect(Opener opener) {
            throw new ConnectionRejectedException("Unexpected IO connection from: " + opener);
        }
    };
}
