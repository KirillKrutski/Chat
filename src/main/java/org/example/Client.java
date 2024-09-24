package org.example;

import java.io.*;
import java.net.*;

public class Client {

    public static void main(String[] args) {
        String hostname = "localhost";  // Адрес сервера
        int port = 8080;  // Порт сервера

        try (Socket socket = new Socket(hostname, port)) {
            // Поток для отправки сообщений на сервер
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // Поток для чтения сообщений с консоли
            BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
            // Поток для чтения сообщений от сервера
            BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Создаем поток для приема сообщений от сервера
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverInput.readLine()) != null) {
                        System.out.println(serverMessage);  // Выводим сообщение от сервера (от других клиентов)
                    }
                } catch (IOException e) {
                    System.out.println("Error from data server: " + e.getMessage());
                }
            }).start();

            // Цикл для отправки сообщений на сервер
            String userInput;
            System.out.println("Enter message (enter 'exit' for disconnect):");
            while ((userInput = consoleInput.readLine()) != null) {
                out.println(userInput);  // Отправляем сообщение на сервер

                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("Client disconnected.");
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Connecting error (server doesn't work): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error in/out: " + e.getMessage());
        }
    }
}
