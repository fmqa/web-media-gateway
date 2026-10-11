package com.github.fmqa.spu.av;

import java.util.List;

/**
 * Process monitoring callbacks.
 */
public interface Monitorable {
    /**
     * Called when a process is started.
     * @param pid process identifier
     * @param argv arguments the process was started with
     */
    void started(long pid, List<String> argv);
    
    /**
     * Called when a process exits.
     * @param pid process identifier
     * @param code the exit code of the process
     */
    void exited(long pid, int code);
}
