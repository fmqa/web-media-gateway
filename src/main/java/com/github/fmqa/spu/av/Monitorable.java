package com.github.fmqa.spu.av;

import java.util.List;

public interface Monitorable {
    void started(long pid, List<String> argv);
    void exited(long pid, int code);
}
