package com.github.fmqa.spu.av;

import java.util.List;

/**
 * Predefined process monitoring callbacks.
 */
public enum Monitorables implements Monitorable {
    /**
     * No-op monitoring callbacks.
     */
    IGNORE {
        @Override
        public void started(long pid, List<String> argv) { /* No-op */ }

        @Override
        public void exited(long pid, int code) { /* No-op */ }
    };
}
