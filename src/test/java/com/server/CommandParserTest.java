package com.server;

import com.cache.CacheStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser(new CacheStore<>());
    }

    @Test
    void setAndGetReturnsStoredValue() {
        CommandResult setResult = parser.handle("SET foo bar");
        assertEquals("+OK", setResult.response());
        assertFalse(setResult.shouldClose());

        CommandResult getResult = parser.handle("GET foo");
        assertEquals("+VALUE bar", getResult.response());
    }

    @Test
    void setWithInvalidTtlReturnsError() {
        CommandResult result = parser.handle("SET foo bar EX invalid");
        assertEquals("-ERR ttl must be a positive number", result.response());
    }

    @Test
    void delReturnsNumberOfRemovedKeys() {
        parser.handle("SET k1 v1");
        parser.handle("SET k2 v2");

        CommandResult delResult = parser.handle("DEL k1 k2 k3");
        assertEquals(":2", delResult.response());
    }

    @Test
    void ttlReturnsRemainingSeconds() {
        parser.handle("SET exp value EX 5");
        CommandResult ttlResult = parser.handle("TTL exp");

        assertTrue(ttlResult.response().startsWith(":"));
        long seconds = Long.parseLong(ttlResult.response().substring(1));
        assertTrue(seconds >= 0 && seconds <= 5);
    }

    @Test
    void dumpListsAllEntries() {
        parser.handle("SET user john");

        CommandResult dumpResult = parser.handle("DUMP");
        assertTrue(dumpResult.response().startsWith("+DUMP "));
        assertTrue(dumpResult.response().contains("user=john"));
    }

    @Test
    void helpReturnsCommandList() {
        CommandResult result = parser.handle("HELP");
        assertTrue(result.response().startsWith("+HELP "));
        assertTrue(result.response().contains("SET"));
    }

    @Test
    void quitClosesConnection() {
        CommandResult result = parser.handle("QUIT");
        assertEquals("+BYE", result.response());
        assertTrue(result.shouldClose());
    }
}

