package me.ericballard.sslchat.network.client;

import me.ericballard.sslchat.SSLChat;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class Client extends Thread {

    SSLSocket socket;

    SSLChat app;

    public Client(SSLChat app) {
        this.app = app;
    }


    @Override
    public void run() {
        try {
            System.out.println("Connected to server: " + socket.getInetAddress());

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Authorize our client and register name on server
            writer.println("CONNECT:" + app.username);

            main:
            while (true) {
                // Wait for server response
                String serverReponse = reader.readLine();

                if (serverReponse == null || !serverReponse.contains(":")) {
                    System.out.println("Invalid Server Response: " + serverReponse);
                    break;
                }

                switch (serverReponse.split(":")[0]) {
                    case "CONNECT-ACCEPTED":
                        // Client has successfully connected to server and registered name
                        break;
                    case "CONNECT-DENIED":
                        // Client's defined username is already taken
                        // Inform server client is disconnecting
                        writer.println("DISCONNECT");
                        break main;
                    case "":
                        break;
                }
            }

            socket.close();
            System.out.println("Closed server connection.");
        } catch (IOException e) {
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
