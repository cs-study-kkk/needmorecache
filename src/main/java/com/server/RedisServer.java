package com.server;

public class RedisServer {
    private final ServerConfig config;

    public RedisServer(ServerConfig config) {
        this.config = config;
    }

    public static RedisServer createDefault() {
        return new RedisServer(ServerConfig.defaultConfig());
    }

    public void start() {
        System.out.printf("MiniRedis Server starting on port %d with %d worker threads...%n",
                config.port(), config.workerThreads());
    }

    public ServerConfig getConfig() {
        return config;
    }
}
