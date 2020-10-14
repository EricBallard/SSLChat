package me.ericballard.sslchat.network.server;

import javafx.application.Platform;
import me.ericballard.sslchat.SSLChat;

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

    // Connected users to server
    public ArrayList<String> connectedClients = new ArrayList<>();

    // Data - Clients to send to
    public HashMap<String, ArrayList<String>> dataToSend = new HashMap<>();

    public Server(SSLChat app) {
        this.app = app;
    }

    @Override
    public void run() {
        started = true;
        connectedClients.add(app.username);

        System.out.println("Opened Secured Server on port 25565 | " + app.username);
        Platform.runLater(() -> app.controller.onlineTxt.setText(String.valueOf(connectedClients.size())));

        while (!isInterrupted()) {
            try {
                // Receive new client
                if (!connectToClient())
                    continue;

                // Handle new client
                new ClientHandler(this, socket).start();
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

            System.out.println("Received connection request: " + socket.getInetAddress());
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
            // Generating a keystore
            //https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html

            // Direct system to keystore
            // (https://docs.oracle.com/cd/E29585_01/PlatformServices.61x/security/src/csec_ssl_jsp_start_server.html)
            System.setProperty("javax.net.ssl.keyStore", "C:\\Users\\Home\\sslchatstore.store");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");

            // Create SSL server socket
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            serverSocket = (SSLServerSocket) ssf.createServerSocket();

            // Configure socket
            //serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(25565));

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
