package com.server;

import com.cache.CacheEntry;
import com.cache.CacheStore;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class CommandParser {
    private final CacheStore<String, String> cacheStore;

    public CommandParser(CacheStore<String, String> cacheStore) {
        this.cacheStore = cacheStore;
    }

    public CommandResult handle(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) {
            return CommandResult.error("empty command");
        }

        List<String> tokens = List.of(rawCommand.trim().split("\\s+"));
        String command = tokens.get(0).toUpperCase(Locale.ROOT);

        return switch (command) {
            case "PING" -> CommandResult.reply("+PONG");
            case "QUIT" -> CommandResult.closing("+BYE");
            case "SET" -> handleSet(tokens);
            case "GET" -> handleGet(tokens);
            case "DEL" -> handleDel(tokens);
            case "EXISTS" -> handleExists(tokens);
            case "TTL" -> handleTtl(tokens);
            case "DUMP" -> handleDump();
            case "HELP" -> handleHelp();
            default -> CommandResult.error("unknown command");
        };
    }

    private CommandResult handleSet(List<String> tokens) {
        if (tokens.size() < 3) {
            return CommandResult.error("SET requires key and value");
        }

        String key = tokens.get(1);
        String value = tokens.get(2);
        long ttlMillis = -1;

        if (tokens.size() > 3) {
            if (tokens.size() != 5 || !"EX".equalsIgnoreCase(tokens.get(3))) {
                return CommandResult.error("SET syntax: SET key value [EX seconds]");
            }
            long ttlSeconds;
            try {
                ttlSeconds = parsePositiveLong(tokens.get(4), "ttl");
            } catch (IllegalArgumentException e) {
                return CommandResult.error(e.getMessage());
            }
            ttlMillis = TimeUnit.SECONDS.toMillis(ttlSeconds);
        }

        if (ttlMillis > 0) {
            cacheStore.set(key, value, ttlMillis);
        } else {
            cacheStore.set(key, value);
        }
        return CommandResult.reply("+OK");
    }

    private CommandResult handleGet(List<String> tokens) {
        if (tokens.size() != 2) {
            return CommandResult.error("GET requires a key");
        }
        String key = tokens.get(1);
        String value = cacheStore.get(key);
        return value != null ? CommandResult.value(value) : CommandResult.error("key not found");
    }

    private CommandResult handleDel(List<String> tokens) {
        if (tokens.size() < 2) {
            return CommandResult.error("DEL requires at least one key");
        }
        long removed = 0;
        for (int i = 1; i < tokens.size(); i++) {
            if (cacheStore.del(tokens.get(i))) {
                removed++;
            }
        }
        return CommandResult.reply(":" + removed);
    }

    private CommandResult handleExists(List<String> tokens) {
        if (tokens.size() != 2) {
            return CommandResult.error("EXISTS requires a key");
        }
        boolean exists = cacheStore.exists(tokens.get(1));
        return CommandResult.reply(":" + (exists ? 1 : 0));
    }

    private CommandResult handleTtl(List<String> tokens) {
        if (tokens.size() != 2) {
            return CommandResult.error("TTL requires a key");
        }
        long ttl = cacheStore.ttl(tokens.get(1));
        if (ttl < 0) {
            return CommandResult.reply(":-1");
        }
        return CommandResult.reply(":" + TimeUnit.MILLISECONDS.toSeconds(ttl));
    }

    private CommandResult handleDump() {
        Map<String, CacheEntry<String>> snapshot = cacheStore.dumpAll();
        if (snapshot.isEmpty()) {
            return CommandResult.reply("+EMPTY");
        }
        StringJoiner joiner = new StringJoiner(" ");
        snapshot.forEach((key, entry) -> joiner.add(key + "=" + entry.getValue()));
        return CommandResult.reply("+DUMP " + joiner);
    }

    private CommandResult handleHelp() {
        String commands = "PING, QUIT, SET, GET, DEL, EXISTS, TTL, DUMP, HELP";
        return CommandResult.reply("+HELP " + commands);
    }

    private long parsePositiveLong(String token, String fieldName) {
        try {
            long value = Long.parseLong(token);
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive number");
        }
    }
}
