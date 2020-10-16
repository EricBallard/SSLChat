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

    public LinkedList<String> dataToSend = new LinkedList<>();

    public boolean typing, disconnecting;

    String address, port;

    public Client(SSLChat app, String address, String port) {
        this.app = app;
        this.address = address;
        this.port = port;
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
                    if (typing)
                        writer.println("IDLE:" + app.username);

                    writer.println("DISCONNECT:" + app.username);
                    break;
                }

                // Check if local client is typing and update server if so
                if (app.controller.typing) {
                    if (!typing)
                        writer.println("TYPING:" + app.username);
                } else if (typing) {
                    writer.println("IDLE:" + app.username);
                } else {
                    // Send messages to server
                    if (!dataToSend.isEmpty()) {
                        String msg = dataToSend.getFirst();
                        dataToSend.removeFirst();
                        writer.println(msg);
                    }
                }

                this.typing = app.controller.typing;
            }

            socket.close();
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

                // Request name is in use
                app.alerts.inform("UNABLE TO CONNECT TO SERVER", "Your requested username is in use, please try again.");
                app.controller.textField.setDisable(false);
                break;
            case "CONNECT-ACCEPTED":
                // Client has successfully connected to server and registered name
                break;
            case "MESSAGE":
                // Add message to local client
                String[] msgInfo = info[1].split(";");
                String username = msgInfo[1];

                app.addMessage(msgInfo[0], username, msgInfo[2], (!app.muted && !username.equals(app.username)));

                if (app.typingClients.contains(username)) {
                    app.typingClients.remove(username);
                    app.updateTypingCount();
                }
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
            while (!isInterrupted()) {
                try {
                    String data = reader.readLine();

                    if (data == null || !data.contains(":")) {
                        continue;
                    }

                    System.out.println("Received data: " + data);
                    receivedData.add(data);
                } catch (IOException e) {
                    String msg = e.getMessage();
                    System.out.println("Failed to read data from client due to: " + msg);

                    if (msg.equals("Socket is closed"))
                        break;
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
