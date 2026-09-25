package com.github.fmqa.spu.io;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Used within server application to perform loopback calls to own API endpoints.
 * <p></p>
 * This component can only be used after the web server is started.
 */
@Component
@Lazy
public class Loopback {
    private final URI location;

    public Loopback(@Value("${local.server.port}") int port) {
        location = URI.create("http://127.0.0.1:" + port);
    }

    /**
     * Returns the root loopback URI for this server. The URI scheme is set to http, the host is set to the loopback
     * address or host, and the port is set to the running server's port.
     * @return A root URI referencing the currently-running server
     */
    public URI uri() {
        return location;
    }
}
