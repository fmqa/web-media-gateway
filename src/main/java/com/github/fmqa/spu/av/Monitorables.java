package com.github.fmqa.spu.av;

import java.util.List;

public enum Monitorables implements Monitorable {
    IGNORE {
        @Override
        public void started(long pid, List<String> argv) { /* No-op */ }

        @Override
        public void exited(long pid, int code) { /* No-op */ }
    };
}
