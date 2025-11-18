package com;

import com.server.RedisServer;
import com.server.ServerConfig;

public class Main {
    public static void main(String[] args) {
        // Example:
        //   ./gradlew run --args="--port=6380 --workers=32"
        //   (IntelliJ → Run Configuration → Program arguments)
        // This builds ServerConfig from CLI args and starts RedisServer.
        ServerConfig config = resolveConfig(args);
        RedisServer server = new RedisServer(config);
        server.start();
    }

    private static ServerConfig resolveConfig(String[] args) {
        ServerConfig.Builder builder = ServerConfig.builder();

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                builder.port(parsePositiveInt(arg.substring(7), "port"));
            } else if (arg.startsWith("--backlog=")) {
                builder.backlog(parsePositiveInt(arg.substring(10), "backlog"));
            } else if (arg.startsWith("--workers=")) {
                builder.workerThreads(parsePositiveInt(arg.substring(10), "workers"));
            }
        }

        return builder.build();
    }

    private static int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(name + " must be positive.");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + " value: " + value, e);
        }
    }
}