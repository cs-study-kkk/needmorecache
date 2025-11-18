package com.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
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
                String response = handleCommand(line);
                writer.write(response + "\r\n");
                writer.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    private String handleCommand(String line) {
        return switch (line.trim().toUpperCase()) {
            case "PING" -> "+PONG";
            case "QUIT" -> "+BYE";
            default -> "-ERR unknown command";
        };
    }
}