package me.ericballard.sslchat.network.server;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ClientHandler extends Thread {

    Server server;
    SSLSocket socket;

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

            // Client is connected and authorization
            main:
            while (true) {
                clientMsg = reader.readLine();

                if (clientMsg == null || !clientMsg.contains(":")) {
                    System.out.println("Invalid Client Message: " + clientMsg);
                    break;
                }

                switch (clientMsg.split(":")[0]) {
                    // Disconnect client from server
                    case "DISCONNECT":
                        System.out.println("Client disconnected: " + socket.getInetAddress());
                        server.connectedClients.remove(name);
                        break main;
                    case "USER-COUNT":
                        int users = server.connectedClients.size();
                        writer.println("USER-COUNT:" + users);
                        break;
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
