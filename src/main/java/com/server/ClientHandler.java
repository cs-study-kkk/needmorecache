package com.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandParser commandParser;

    public ClientHandler(Socket socket, CommandParser commandParser) {
        this.socket = socket;
        this.commandParser = commandParser;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            writer.write("+OK MiniRedis ready\r\n");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                CommandResult result = commandParser.handle(line);
                writer.write(result.response() + "\r\n");
                writer.flush();
                if (result.shouldClose()) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}