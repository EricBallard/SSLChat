package me.ericballard.sslchat.network.server;

import javafx.application.Platform;
import me.ericballard.sslchat.SSLChat;
import me.ericballard.sslchat.network.Keystore;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Thread {

    public boolean started;

    SSLChat app;
    SSLSocket socket;
    SSLServerSocket serverSocket;

    String port;

    public boolean disconnecting;

    // Connected users to server
    public ArrayList<String> connectedClients = new ArrayList<>();

    // Data - Clients to send to
    public HashMap<String, ArrayList<String>> dataToSend = new HashMap<>();

    public ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    public Server(SSLChat app, String port) {
        this.app = app;
        this.port = port;
    }

    @Override
    public void run() {
        started = true;
        connectedClients.add(app.username);

        System.out.println("Opened Secured Server on port " + port + " | " + app.username);
        Platform.runLater(() -> app.controller.onlineTxt.setText(String.valueOf(connectedClients.size())));

        while (!isInterrupted()) {
            try {
                // Receive new client
                if (!connectToClient())
                    continue;

                // Handle new client
                ClientHandler handler = new ClientHandler(this, socket);
                handler.start();

                // Cached handler to update media queue
                clientHandlers.add(handler);
            } catch (Exception e) {
                if (!started)
                    break;

                e.printStackTrace();
            }
        }

        close();
    }

    public boolean connectToClient() {
        try {
            Socket clientSocket = serverSocket.accept();

            // Reject any non ssl clients
            if (!(clientSocket instanceof SSLSocket)) {
                System.out.println("Rejected - client is not instance of SSL!");
                return false;
            }


            socket = (SSLSocket) clientSocket;
            socket.setKeepAlive(true);

            //System.out.println("Received connection request: " + socket.getInetAddress());
            return true;
        } catch (IOException e) {
            if (started) {
                System.out.println("Server failed to deploy or connect to client due to: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    public void initialize() {
        try {
            if (!Keystore.initialize())
                throw new IOException("Failed to initialize KeyStore!");

            // Create SSL server socket
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            serverSocket = (SSLServerSocket) ssf.createServerSocket();

            // Configure socket
            //serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(Integer.parseInt(port)));

            // Start thread for server
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        System.out.println("Shutting-down Secured Server!");
        started = false;
        interrupt();
    }
}
