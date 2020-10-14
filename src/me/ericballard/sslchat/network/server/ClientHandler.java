package me.ericballard.sslchat.network.server;

import javafx.application.Platform;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;

public class ClientHandler extends Thread {

    Server server;
    SSLSocket socket;
    LinkedList<String> receivedData = new LinkedList<>();

    public ClientHandler(Server server, SSLSocket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // A new client has connected to the server
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Wait for client to send username
            String clientMsg = reader.readLine();

            // Validate authorization
            if (clientMsg == null || !clientMsg.startsWith("CONNECT:")) {
                System.out.println("Invalid connection request: " + clientMsg);
                writer.println("CONNECT-DENIED:Invalid message.");
                socket.close();
                return;
            }

            String username = clientMsg.replace("CONNECT:", "");
            String name = username.toUpperCase();

            // Check if username is reserved by other client
            if (server.connectedClients.contains(name)) {
                System.out.println("Unable to connect client, username is in use: " + clientMsg);
                writer.println("CONNECT-DENIED:Username is in use.");
                socket.close();
                return;
            }

            // Username is available, cache name
            server.connectedClients.add(name);
            writer.println("CONNECT-ACCEPTED:Registered username!");

            // Update connected clients new user joined
            ArrayList<String> clientsToInform = new ArrayList<>();
            for (String client : server.connectedClients) {
                if (!client.equals(name))
                    clientsToInform.add(client);
            }

            int online = server.connectedClients.size();
            String userUpdate = "USER-COUNT:" + online;

            server.dataToSend.put(userUpdate, clientsToInform);

            // Update local user # of connected clients
            Platform.runLater(() -> server.app.controller.onlineTxt.setText(String.valueOf(online)));

            // Start separate thread to cache incoming data from client
            Thread readThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try {
                        String data = reader.readLine();

                        if (data == null || !data.contains(":")) {
                            System.out.println("Invalid data: " + data);
                            continue;
                        }

                        System.out.println("Received data: " + data);
                        receivedData.add(data);
                    } catch (IOException e) {
                        String msg = e.getMessage();
                        System.out.println("Failed to read data from client due to: " + msg);

                        if (msg.equals("Socket is closed"))
                            interrupt();
                    }
                }
            });

            readThread.start();

            // Client is connected and authorization
            main:
            while (!isInterrupted() && !readThread.isInterrupted()) {
                if (!receivedData.isEmpty()) {
                    clientMsg = receivedData.getFirst();
                    receivedData.removeFirst();

                    String[] data = clientMsg.split(":");
                    switch (data[0]) {
                        // Disconnect client from server
                        case "DISCONNECT":
                            readThread.interrupt();
                            server.connectedClients.remove(name);

                            System.out.println("Client disconnected: " + socket.getInetAddress());

                            // Inform connected users client has left
                            int on = server.connectedClients.size();
                            server.dataToSend.put("USER-COUNT:" + on, server.connectedClients);

                            // Update local user count
                            Platform.runLater(() -> server.app.controller.onlineTxt.setText(String.valueOf(on)));
                            break main;
                        case "USER-COUNT":
                            writer.println("USER-COUNT:" + server.connectedClients.size());
                            continue main;
                        case "TYPING":
                            String userTyping = data[1];
                            clientsToInform = new ArrayList<>();
                            for (String client : server.connectedClients) {
                                if (!client.equals(name) && !client.equals(userTyping))
                                    clientsToInform.add(client);
                            }

                            // Client has informed server they are typing - inform all connected users that client is typing
                            server.dataToSend.put("TYPING:" + userTyping, clientsToInform);

                            // Update typing count for local client
                            server.app.typingClients.add(userTyping);
                            server.app.updateTypingCount();
                            break;
                        case "IDLE":
                            // Client has connected and authorized - waiting for data
                            // (Eg; user typing / new message to server)
                            break;
                    }
                }

                // Update each user when a connected client types or sends message
                if (server.dataToSend.isEmpty()) {
                    //System.out.println("no data to send..");
                    continue main;
                }

                // Check if need to update client
                String dataToSend = null;

                try {
                    dataLoop:
                    for (String data : server.dataToSend.keySet()) {
                        if (server.dataToSend.get(data).contains(name)) {
                            dataToSend = data;
                            break dataLoop;
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    continue main;
                }

                // Connected client is fully updated
                if (dataToSend == null)
                    continue main;

                // Client has data to receive
                writer.println(dataToSend);

                if (server.dataToSend.get(dataToSend).size() == 1)
                    server.dataToSend.remove(dataToSend);
                else
                    server.dataToSend.get(dataToSend).remove(name);

                continue main;
            }

            System.out.println("CLIENT DISCONNECTING...");

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
