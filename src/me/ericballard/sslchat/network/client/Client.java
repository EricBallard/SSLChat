package me.ericballard.sslchat.network.client;

import javafx.application.Platform;
import me.ericballard.sslchat.SSLChat;
import me.ericballard.sslchat.network.Keystore;
import me.ericballard.sslchat.network.Media;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.LinkedList;

public class Client extends Thread {

    SSLChat app;

    SSLSocket socket;

    Thread readThread;

    LinkedList<File> mediaQueue = new LinkedList<>();

    LinkedList<String> receivedData = new LinkedList<>();

    public LinkedList<String> dataToSend = new LinkedList<>();

    public boolean typing, disconnecting, receivingImg;

    public File imgToSend;

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
                if (!receivingImg)
                    receiveUpdate();

                // Client is disconnecting
                if (disconnecting) {
                    readThread.interrupt();

                    if (typing)
                        writer.println("IDLE:" + app.username);

                    writer.println("DISCONNECT:" + app.username);

                    // Shutdown test instance
                    if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                        // On linux instance - shutdown
                        Runtime runtime = Runtime.getRuntime();
                        try {
                            runtime.exec("shutdown -h now");
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("(OOPSIE) Failed to shutdown");
                        }
                    }
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
                        dataToSend.remove(msg);

                        if (msg.startsWith("MEDIA")) {
                            writer.println(msg);
                            Media.send(socket, writer, imgToSend);
                        } else
                            writer.println(msg);
                    }
                }

                this.typing = app.controller.typing;
            }

            socket.close();
        } catch (Exception e) {
            String msg = e.getMessage();
            System.out.println("Failed to read data from client due to: " + msg);

            if (msg == null)
                e.printStackTrace();
        }
    }

    private void receiveUpdate() {
        if (receivedData.isEmpty())
            return;

        String serverReponse = receivedData.getFirst();
        receivedData.remove(serverReponse);

        String[] info = serverReponse.split(":");
        String data = info[0];

        switch (data) {
            case "CAPACITY":
                disconnecting = true;
                break;
            case "SHUTDOWN":
                readThread.interrupt();

                app.typingClients.clear();
                app.updateTypingCount();

                Platform.runLater(() -> {
                    app.controller.statusCircle.setFill(app.controller.offFill);
                    app.controller.onlineTxt.setText("0");

                    app.controller.imgTxt.setDisable(true);
                    app.controller.mediaImg.setDisable(true);

                    app.controller.soundTxt.setDisable(true);
                    app.controller.soundImg.setDisable(true);
                });

                interrupt();
                break;
            case "CONNECT-DENIED":
                disconnecting = true;

                // Request name is in use
                app.alerts.inform("UNABLE TO CONNECT TO SERVER", "Your requested username is in use, please try again.");
                app.controller.textField.setDisable(false);
                break;
            case "CONNECT-ACCEPTED":
                // Client has successfully connected to server and registered name
                break;
            case "MEDIA":
                if (mediaQueue.isEmpty())
                    break;

                File file = mediaQueue.getFirst();
                boolean removed = mediaQueue.remove(file);

                System.out.println("REMOVED MEDIA: " + removed + " | " + file.getName() + " ~ " + mediaQueue.size());

                // Add media to local client
                app.addMedia(file, info[1]);
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
                    if (receivingImg) {
                        // Client has sent media - relay to connected clients
                        File media = Media.receive(socket, reader);

                        if (media != null)
                            mediaQueue.add(media);
                        else
                            receivedData.removeLast();

                        receivingImg = false;
                        continue;
                    }

                    String data = reader.readLine();

                    if (data == null || !data.contains(":")) {
                        continue;
                    }

                    System.out.println("Received data: " + data);

                    if (data.startsWith("MEDIA:"))
                        receivingImg = true;

                    receivedData.add(data);
                } catch (IOException e) {
                    String msg = e.getMessage();
                    System.out.println("Failed to read data from client due to: " + msg);

                    if (msg == null)
                        e.printStackTrace();
                    else if (msg.contains("closed"))
                        break;
                }
            }
        });
    }

    public void initialize() {
        try {
            if (!Keystore.initialize())
                throw new IOException("Failed to initialize KeyStore!");

            // Create SSL socket
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(address, Integer.parseInt(port));
            socket.setKeepAlive(true);

            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
