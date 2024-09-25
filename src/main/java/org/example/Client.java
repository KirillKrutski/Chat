package org.example;

import javax.swing.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String clientLogin;

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton editButton;
    private JButton deleteButton;
    private JList<String> messageList;
    private DefaultListModel<String> listModel;

    public Client(String serverHost, int serverPort) {
        JFrame frame = new JFrame("Chat");
        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        messageField = new JTextField(20);
        sendButton = new JButton("Send");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");

        listModel = new DefaultListModel<>();
        messageList = new JList<>(listModel);

        JPanel panel = new JPanel();
        panel.add(messageField);
        panel.add(sendButton);
        panel.add(editButton);
        panel.add(deleteButton);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        frame.getContentPane ().add(new JScrollPane(messageList), BorderLayout.EAST);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        setupNetworking(serverHost, serverPort);

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                if (!message.isEmpty()) {
                    out.println(message);
                    listModel.addElement(message);
                    messageField.setText("");
                }
            }
        });

        editButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               int selectedIndex = messageList.getSelectedIndex();
               if (selectedIndex != -1){
                   String selectedMessage = listModel.getElementAt(selectedIndex);
                   messageField.setText(selectedMessage);
                   listModel.remove(selectedIndex);
               }
           }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = messageList.getSelectedIndex();
                if (selectedIndex != -1){
                    listModel.remove(selectedIndex);
                    out.println("Message deleted");
                }
            }
        });
    }

    private void setupNetworking(String serverHost, int serverPort) {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            clientLogin = JOptionPane.showInputDialog("Enter Client Login");
            out.println(clientLogin);

            Thread listenThread = new Thread(new IncomingReader());
            listenThread.start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private class IncomingReader implements Runnable {
        public void run() {
            String message;
            try{
                while ((message = in.readLine()) != null) {
                    chatArea.append(message + "\n");
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Client("localhost", 8080);
    }
}