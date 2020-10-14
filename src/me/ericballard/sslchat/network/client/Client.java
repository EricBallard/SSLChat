package me.ericballard.sslchat.network.client;

import me.ericballard.sslchat.SSLChat;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.LinkedList;

public class Client extends Thread {

    SSLChat app;

    SSLSocket socket;

    LinkedList<String> receivedData = new LinkedList<>();

    public boolean typing, disconnecting;

    public Client(SSLChat app) {
        this.app = app;
    }

    @Override
    public void run() {
        try {
            System.out.println("Connecting to server: " + socket.getInetAddress());

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Authorize our client and register name on server
            writer.println("CONNECT:" + app.username);

            // Start separate thread to cache incoming data from server
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
                        System.out.println("Failed to read data from server due to: " + e.getMessage());
                    }
                }
            });

            readThread.start();

            main:
            while (!Thread.interrupted()) {
                // Client is disconnecting
                if (disconnecting) {
                    System.out.println("Disconnecting from server...");
                    writer.println("DISCONNECT:" + app.username);
                    System.out.println("Sent data, interuppting threads..");

                    readThread.interrupt();
                    //interrupt();
                    break;
                }

                if (!receivedData.isEmpty()) {
                    String serverReponse = receivedData.getFirst();
                    receivedData.removeFirst();

                    String[] data = serverReponse.split(":");

                    switch (data[0]) {
                        case "CONNECT-DENIED":
                            // Client's defined username is already taken
                            // Inform server client is disconnecting
                            writer.println("DISCONNECT:Client");
                            break main;
                        case "CONNECT-ACCEPTED":
                            // Client has successfully connected to server and registered name

                            // Request number of users connected to server
                            writer.println("USER-COUNT:Request");
                            break;
                        case "USER-COUNT":
                            // Client has requested number of connected users
                            String connectedClients = data[1];
                            int users = 0;

                            try {
                                users = Integer.parseInt(connectedClients);
                            } catch (NumberFormatException ne) {
                                System.out.println("Failed to retrieve number of connected clients on server: " + serverReponse);
                            }

                            app.controller.onlineTxt.setText(String.valueOf(users));

                            // Inform server we've received data, wait for further info (eg; user typing/new message)
                            writer.println("IDLE:Client");
                            break;
                        case "TYPING":
                            String userTyping = data[1];
                            app.typingClients.add(userTyping);
                            app.updateTypingCount();
                            break;
                    }
                }

                // Check if local client is typing and update server if so
                if (!this.typing && app.controller.typing)
                    writer.println("TYPING:" + app.username);

                this.typing = app.controller.typing;
                Thread.sleep(200);
            }

            socket.close();
            System.out.println("Closed server connection.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialize() {
        try {
            // Direct system to keystore
            // (https://docs.oracle.com/cd/E29585_01/PlatformServices.61x/security/src/csec_ssl_jsp_start_server.html)
            System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\Home\\sslchatstore.store");
            System.setProperty("javax.net.ssl.trustStorePassword ", "sslchatstore.store");

            // Create SSL socket
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket("127.0.0.1", 25565);
            socket.setKeepAlive(true);

            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
