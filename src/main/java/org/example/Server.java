package org.example;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static JTextArea serverMessages;
    // Карта для хранения логинов клиентов и их потоков вывода
    private static Map<String, PrintWriter> clientMap = new HashMap<>();

    public static void main(String[] args) {
        JFrame frame = new JFrame("Server");
        serverMessages = new JTextArea(20, 40);
        serverMessages.setEditable(false);
        JScrollPane scroll = new JScrollPane(serverMessages);
        frame.add(scroll);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int port = 8080;  // Порт сервера

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            appendMessage("Server start by port: " + port);

            // Бесконечный цикл для обработки подключений клиентов
            while (true) {
                Socket clientSocket = serverSocket.accept();  // Принятие клиента
                appendMessage("Client connected: " + clientSocket.getInetAddress());
                // Создаем новый поток для клиента
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            appendMessage("Error server work: " + e.getMessage());
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
                out = new PrintWriter(socket.getOutputStream(), true); // Поток вывода для клиента
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Поток для чтения сообщений клиента
                out.println("Enter your login:"); // Запрос логина у клиента
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

                appendMessage("User has been connected to chat, " + clientLogin);

                String message;
                // Чтение сообщений клиента
                while ((message = in.readLine()) != null) {
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
                        appendMessage(clientLogin + ": " + message);
                    }
                }
            } catch (IOException e) {
                appendMessage("Error client working: " + e.getMessage());
            } finally {
                // Удаление клиента из карты при отключении
                if (clientLogin != null) {
                    synchronized (clientMap) {
                        clientMap.remove(clientLogin);
                        appendMessage("User " + clientLogin + " has been disconnected.");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    appendMessage("Error of client's socket: " + e.getMessage());
                }
            }
        }
    }
    public static void appendMessage(String text) {
        serverMessages.append(text + "\n");
    }
}
