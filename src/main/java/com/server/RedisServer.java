package com.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private final ServerConfig config;
    private final ExecutorService clientPool;

    public RedisServer(ServerConfig config) {
        this.config = config;
        this.clientPool = Executors.newFixedThreadPool(config.workerThreads());
    }

    public static RedisServer createDefault() {
        return new RedisServer(ServerConfig.defaultConfig());
    }

    public ServerConfig getConfig() {
        return config;
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(config.port(), config.backlog())) {
            System.out.printf("MiniRedis Server started on port %d...%n", config.port());
            acceptLoop(serverSocket);
        } catch (IOException e) {
            throw new IllegalStateException("Server failed to start", e);
        } finally {
            clientPool.shutdownNow();
        }
    }

    private void acceptLoop(ServerSocket serverSocket) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            Socket client = serverSocket.accept();
            clientPool.submit(new ClientHandler(client));
        }
    }
}
