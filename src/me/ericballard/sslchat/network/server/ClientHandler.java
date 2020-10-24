package me.ericballard.sslchat.network.server;

import javafx.application.Platform;
import me.ericballard.sslchat.network.Media;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.*;

public class ClientHandler extends Thread {

    Server server;
    SSLSocket socket;
    Thread readThread;
    boolean receivingImg;

    LinkedList<String> receivedData = new LinkedList<>();
    public LinkedList<File> mediaQueue = new LinkedList<>();


    public ClientHandler(Server server, SSLSocket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        String name = null;

        try {
            // A new client has connected to the server
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Authorize clients requested name
            name = authorize(writer, reader);

            if (name != null) {
                // Update connected user #
                updateConnectedClientSize();

                // Start separate thread to cache incoming data from client
                readThread = initReadThread(reader);
                readThread.start();

                // Client is connected and authorization
                while (!isInterrupted() && !readThread.isInterrupted()) {
                    if (!server.disconnecting && !receivingImg && !receiveUpdate(name)) {
                        // User is dis-connecting
                        break;
                    }

                    // Send updates to client (users typing/messages/connecting/etc)
                    if (!sendUpdate(writer, name)) {
                        // Server is shutting-down
                        readThread.interrupt();
                        break;
                    }
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.clientHandlers.remove(this);

            if (name != null)
                server.connectedClients.remove(name);
        }
    }


    private Thread initReadThread(BufferedReader reader) {
        return new Thread(() -> {
            while (!isInterrupted()) {
                try {
                    if (receivingImg) {
                        // Client has sent media - relay to connected clients
                        File media = Media.receive(socket, reader);

                        if (media != null) {
                            // Populate client's media queue
                            server.clientHandlers.forEach(client -> client.mediaQueue.add(media));
                        } else
                            receivedData.removeLast();

                        receivingImg = false;
                        continue;
                    }

                    String data = reader.readLine();

                    if (data == null || !data.contains(":"))
                        continue;

                    System.out.println("Received data: " + data);

                    if (data.startsWith("MEDIA:"))
                        receivingImg = true;

                    receivedData.add(data);
                } catch (IOException e) {
                    String msg = e.getMessage();
                    System.out.println("Failed to read data from client due to: " + msg);

                    if (msg == null)
                        e.printStackTrace();

                    break;
                }
            }
        });
    }

    private String authorize(PrintWriter writer, BufferedReader reader) throws IOException {
        // Wait for client to send username
        String clientMsg = reader.readLine();

        // Validate authorization
        if (clientMsg == null || !clientMsg.startsWith("CONNECT:")) {
            System.out.println("Invalid connection request: " + clientMsg);
            writer.println("CONNECT-DENIED:Invalid message.");
            socket.close();
            return null;
        }

        String username = clientMsg.replace("CONNECT:", "");
        String name = username.toLowerCase();

        // Check if username is reserved by other client
        if (server.connectedClients.contains(name)) {
            System.out.println("Unable to connect client, username is in use: " + clientMsg);
            writer.println("CONNECT-DENIED:Username is in use.");
            socket.close();
            return null;
        }

        // Username is available, cache name
       server.connectedClients.add(name);
        writer.println("CONNECT-ACCEPTED:Registered username!");
        return name;
    }


    private void updateConnectedClientSize() {
        // Update connected clients new user joined
        ArrayList<String> clientsToInform = (ArrayList<String>) server.connectedClients.clone();
        clientsToInform.remove(server.app.username);

        int online = server.connectedClients.size();
        String userUpdate = "USER-COUNT:" + online;
        server.dataToSend.put(userUpdate, clientsToInform);

        // Update local user # of connected clients
        Platform.runLater(() -> server.app.controller.onlineTxt.setText(String.valueOf(online)));
    }

    private boolean receiveUpdate(String name) {
        if (receivedData.isEmpty())
            return true;

        String clientMsg = receivedData.getFirst();
        receivedData.removeFirst();

        ArrayList<String> clientsToInform;
        String[] info = clientMsg.split(":");
        String data = info[0];

        // Fix to include typed messages containing : char
        if (info.length > 2) {
            String messages = null;
            for (int i = 1; i < info.length; i++)
                messages = (messages == null ? "" : messages + ":") + info[i];
            info = new String[]{data, messages};
        }

        switch (data) {
            case "CAPACITY":
                // Stress-test concluded - relay message to users
                clientsToInform = (ArrayList<String>) server.connectedClients.clone();
                clientsToInform.remove(server.app.username);

                server.dataToSend.put(clientMsg, clientsToInform);
                break;
            // Disconnect client from server
            case "DISCONNECT":
                readThread.interrupt();
                server.connectedClients.remove(name);

                Iterator<String> itr = server.dataToSend.keySet().iterator();

                while (itr.hasNext()) {
                    String s = itr.next();
                    clientsToInform = server.dataToSend.get(s);

                    if (clientsToInform.contains(name))
                        clientsToInform.remove(name);
                }

                updateConnectedClientSize();

                if (server.app.typingClients.contains(name)) {
                    server.app.typingClients.remove(name);
                    server.app.updateTypingCount();
                }
                return false;
            case "MEDIA":
                if (mediaQueue.isEmpty())
                    break;

                // Add media to local client
                File file = mediaQueue.getFirst();
                server.app.addMedia(file, info[1]);

                // Relay message to users
                clientsToInform = (ArrayList<String>) server.connectedClients.clone();
                clientsToInform.remove(server.app.username);

                server.dataToSend.put(clientMsg, clientsToInform);
                break;
            case "MESSAGE":
                // Client has sent new message - relay to connected clients
                clientsToInform = (ArrayList<String>) server.connectedClients.clone();
                clientsToInform.remove(server.app.username);

                server.dataToSend.put(clientMsg, clientsToInform);

                // Add message to local client
                String[] msgInfo = info[1].split(";");
                server.app.addMessage(msgInfo[0], msgInfo[1], msgInfo[2], !server.app.muted);
                break;
            case "IDLE":
            case "TYPING":
                // Client has informed server they are typing - inform all connected users that client is typing
                String username = info[1];

                clientsToInform = (ArrayList<String>) server.connectedClients.clone();
                clientsToInform.remove(username);

                server.dataToSend.put(data + ":" + username, clientsToInform);

                if (data.equals("TYPING")) {
                    // Update typing count for local client
                    server.app.typingClients.add(username);
                } else {
                    server.app.typingClients.remove(username);
                }

                server.app.updateTypingCount();
                break;
        }
        return true;
    }

    private boolean sendUpdate(PrintWriter writer, String name) {
        if (server.dataToSend.isEmpty()) {
            return !server.disconnecting;
        }

        // Check if need to update client
        String dataToSend = null;
        ArrayList<String> toInform = null;

        try {
            Iterator<String> itr = server.dataToSend.keySet().iterator();

            while (itr.hasNext()) {
                String data = itr.next();
                toInform = server.dataToSend.get(data);

                if (toInform == null || toInform.isEmpty()) {
                    itr.remove();
                } else if (toInform.contains(name)) {
                    dataToSend = data;
                    break;
                }
            }
        } catch (ConcurrentModificationException e) {
            return true;
        }

        // Connected client is fully updated
        if (dataToSend == null)
            return true;

        // Client needs to be updated
        System.out.println(name + " | Sending data: " + dataToSend);
        writer.println(dataToSend);

        if (dataToSend.startsWith("MEDIA:")) {
            File file = mediaQueue.getFirst();
            boolean removed = mediaQueue.remove(file);

            System.out.println("REMOVED MEDIA: " + removed + " | " + file.getName() + " ~ " + mediaQueue.size());

            Media.send(socket, writer, file);
        }

        if (toInform.size() == 1)
            server.dataToSend.remove(dataToSend);
        else
            toInform.remove(name);

        return true;
    }
}
