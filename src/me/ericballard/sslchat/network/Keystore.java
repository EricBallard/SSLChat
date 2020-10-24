package me.ericballard.sslchat.network;

import me.ericballard.sslchat.SSLChat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Keystore {

    public static boolean initialize() {
        // Copy to local if file doesnt exist
        String path = Media.path + "sslchatstore.store";
        File file = new File(path);

        if (!file.exists()) {
            System.out.println("(SSL) Writing KeyStore to local disk...");
            File folder = new File(Media.path);

            if (!folder.exists())
                folder.mkdirs();

            try {
                InputStream is = SSLChat.class.getResourceAsStream("/me/ericballard/sslchat/network/sslchatstore.store");
                byte[] bytes = new byte[1959];
                is.read(bytes, 0, 1959);

                Files.write(Paths.get(file.getPath()), bytes);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // Generating a keystore
        //https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html

        // Direct system to keystore
        // (https://docs.oracle.com/cd/E29585_01/PlatformServices.61x/security/src/csec_ssl_jsp_start_server.html)
        System.setProperty("javax.net.ssl.keyStore", path);
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        System.setProperty("javax.net.ssl.trustStore", path);
        System.setProperty("javax.net.ssl.trustStorePassword", "password");
        return true;
    }
}
