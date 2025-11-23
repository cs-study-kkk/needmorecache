package com.server;

public record CommandResult(String response, boolean shouldClose) {
    public static CommandResult reply(String response) {
        return new CommandResult(response, false);
    }

    public static CommandResult closing(String response) {
        return new CommandResult(response, true);
    }

    public static CommandResult error(String message) {
        return reply("-ERR " + message);
    }

    public static CommandResult value(String value) {
        return reply("+VALUE " + value);
    }
}

