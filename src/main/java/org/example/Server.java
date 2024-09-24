package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    // Карта для хранения логинов клиентов и их потоков вывода
    private static Map<String, PrintWriter> clientMap = new HashMap<>();

    public static void main(String[] args) {
        int port = 8080;  // Порт сервера

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server start by port: " + port);

            // Бесконечный цикл для обработки подключений клиентов
            while (true) {
                Socket clientSocket = serverSocket.accept();  // Принятие клиента
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Создаем новый поток для клиента
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Error server work: " + e.getMessage());
        }
    }

    // Класс для обработки каждого клиента в отдельном потоке
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientLogin;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // Поток вывода для клиента
                out = new PrintWriter(socket.getOutputStream(), true);
                // Поток для чтения сообщений клиента
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Запрос логина у клиента
                out.println("Enter your login:");
                clientLogin = in.readLine();

                // Проверка уникальности логина
                synchronized (clientMap) {
                    while (clientMap.containsKey(clientLogin)) {
                        out.println("This login is already in use. Enter a different login:");
                        clientLogin = in.readLine();
                    }
                    // Добавляем клиента в карту
                    clientMap.put(clientLogin, out);
                }

                out.println("Welcome to Server, " + clientLogin);

                String message;
                // Чтение сообщений клиента
                while ((message = in.readLine()) != null) {
                    System.out.println(clientLogin + ": " + message);

                    // Проверка, адресовано ли сообщение конкретному клиенту
                    if (message.startsWith("@")) {
                        int separatorIndex = message.indexOf(",");
                        if (separatorIndex != -1) {
                            String targetLogin = message.substring(1, separatorIndex).trim();
                            String privateMessage = message.substring(separatorIndex + 1).trim();

                            // Отправка сообщения только указанному клиенту
                            synchronized (clientMap) {
                                if (clientMap.containsKey(targetLogin)) {
                                    PrintWriter targetOut = clientMap.get(targetLogin);
                                    targetOut.println("Private message " + clientLogin + ": " + privateMessage);
                                    out.println("Private message was send to " + targetLogin + ": " + privateMessage);
                                } else {
                                    out.println("Error: user name " + targetLogin + " not found.");
                                }
                            }
                        } else {
                            out.println("Error: wrong of private message. Use: @clientLogin, message...");
                        }
                    } else {
                        // Рассылка сообщения всем клиентам
                        synchronized (clientMap) {
                            for (PrintWriter writer : clientMap.values()) {
                                writer.println(clientLogin + ": " + message);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error client working: " + e.getMessage());
            } finally {
                // Удаление клиента из карты при отключении
                if (clientLogin != null) {
                    synchronized (clientMap) {
                        clientMap.remove(clientLogin);
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error of client's socket: " + e.getMessage());
                }
            }
        }
    }
}
