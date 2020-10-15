package me.ericballard.sslchat.network.client;

import javafx.application.Platform;
import me.ericballard.sslchat.SSLChat;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.LinkedList;

public class Client extends Thread {

    SSLChat app;

    SSLSocket socket;

    Thread readThread;

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

            // Start separate thread to cache incoming data from server
            readThread = initReadThread(reader);
            readThread.start();

            // Authorize our client and register name on server
            writer.println("CONNECT:" + app.username);

            while (!isInterrupted() && !readThread.isInterrupted()) {
                receiveUpdate(writer);

                // Client is disconnecting
                if (disconnecting) {
                    System.out.println("Disconnecting from server...");
                    writer.println("DISCONNECT:" + app.username);
                    System.out.println("Sent data, interuppting threads..");

                    readThread.interrupt();
                    break;
                }

                // Check if local client is typing and update server if so
                if (app.controller.typing) {
                    if (!typing)
                        writer.println("TYPING:" + app.username);
                } else if (typing)
                    writer.println("IDLE:" + app.username);

                this.typing = app.controller.typing;
                Thread.sleep(200);
            }

            socket.close();
            System.out.println("CLIENT DISCONNECTING | SOCKET CLOSED...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveUpdate(PrintWriter writer) {
        if (receivedData.isEmpty())
            return;

        String serverReponse = receivedData.getFirst();
        receivedData.removeFirst();

        String[] info = serverReponse.split(":");
        String data = info[0];

        switch (data) {
            case "CONNECT-DENIED":
                disconnecting = true;
                break;
            case "CONNECT-ACCEPTED":
                // Client has successfully connected to server and registered name
                break;
            case "USER-COUNT":
                // Client has requested number of connected users
                String connectedClients = info[1];
                int users;

                try {
                    users = Integer.parseInt(connectedClients);
                } catch (NumberFormatException ne) {
                    System.out.println("Failed to retrieve number of connected clients on server: " + serverReponse);
                    break;
                }

                Platform.runLater(() -> app.controller.onlineTxt.setText(String.valueOf(users)));
                break;
            case "IDLE":
            case "TYPING":
                String userTyping = info[1];

                if (data.equals("TYPING")) {
                    if (app.typingClients.contains(userTyping))
                        break;

                    app.typingClients.add(userTyping);
                } else {
                    app.typingClients.remove(userTyping);
                }

                app.updateTypingCount();
                break;
        }
    }

    private Thread initReadThread(BufferedReader reader) {
        return new Thread(() -> {
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
