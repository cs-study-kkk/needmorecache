package com.server;

import java.time.Duration;

public class ServerConfig {
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_BACKLOG = 50;
    private static final int DEFAULT_WORKER_THREADS = 16;
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final int port;
    private final int backlog;
    private final int workerThreads;
    private final Duration shutdownTimeout;

    private ServerConfig(Builder builder) {
        this.port = builder.port;
        this.backlog = builder.backlog;
        this.workerThreads = builder.workerThreads;
        this.shutdownTimeout = builder.shutdownTimeout;
    }

    public static Builder builder() {
        return new Builder()
                .port(DEFAULT_PORT)
                .backlog(DEFAULT_BACKLOG)
                .workerThreads(DEFAULT_WORKER_THREADS)
                .shutdownTimeout(DEFAULT_SHUTDOWN_TIMEOUT);
    }

    public static ServerConfig defaultConfig() {
        return builder().build();
    }

    public int port() {
        return port;
    }

    public int backlog() {
        return backlog;
    }

    public int workerThreads() {
        return workerThreads;
    }

    public Duration shutdownTimeout() {
        return shutdownTimeout;
    }

    public static final class Builder {
        private int port = DEFAULT_PORT;
        private int backlog = DEFAULT_BACKLOG;
        private int workerThreads = DEFAULT_WORKER_THREADS;
        private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder backlog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        public Builder workerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        public Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public ServerConfig build() {
            return new ServerConfig(this);
        }
    }
}

