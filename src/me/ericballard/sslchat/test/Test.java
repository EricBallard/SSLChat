package me.ericballard.sslchat.test;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import me.ericballard.sslchat.SSLChat;
import me.ericballard.sslchat.network.Media;
import me.ericballard.sslchat.network.client.Client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {

    static int desiredCapacity = 8;

    static String ip = "x.x.x.x", port = "25565";

    // Auto configure and simulate user interaction to provide stress test at scale
    public static void execute(SSLChat app) {
        // Generate username
        String username = null;
        Random r = new Random();
        int length = r.ints(1, 10).findFirst().getAsInt();

        for (int i = 0; i < length; i++) {
            char c = (char) (r.nextInt(26) + 'a');
            username = (username != null ? username : "") + c;
        }

        app.username = username;
        System.out.println("(TEST) Generated username: " + username);

        // Generate color
        double rr = r.doubles(0.0, 1.0).findFirst().getAsDouble();
        double gg = r.doubles(0.0, 1.0).findFirst().getAsDouble();
        double bb = r.doubles(0.0, 1.0).findFirst().getAsDouble();

        Color color = Color.color(rr, gg, bb, 1.0);

        // Connect to server
        (app.client = new Client(app, ip, port)).initialize();
        System.out.println("(TEST) Connecting to server...");

        // Wait for handshake
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("(FAILED) Error waiting for server to handshake!");
            System.exit(0);
        }

        // Verify server connection
        if (!app.client.isAlive() || app.client.disconnecting) {
            System.out.println("(FAILED) Error connecting to server!");
            System.exit(0);
            return;
        }

        // Simulate sending periodic messages, until desired user-count is reached
        // Once at capacity, lazily disconnect
        AtomicInteger capacity = new AtomicInteger();
        String userColor = color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getOpacity();

        while (!Thread.interrupted() && app.client.isAlive()) {
            try {
                Platform.runLater(() -> capacity.set(Integer.parseInt(app.controller.onlineTxt.getText())));
                System.out.println("(TEST) CAPACITY: " + capacity.get());

                // Success
                if (capacity.get() >= desiredCapacity) {
                    // Disconnect - waiting a random amount of time before doing so
                    System.out.println("(TEST) Reached capacity!");

                    app.client.dataToSend.add("CAPACITY:Reached");
                    Thread.sleep(r.nextInt(1000));
                    break;
                }

                // Send random text/media messages
                int seed = r.nextInt(100);
                boolean sendMedia = (seed < r.nextInt(10));

                if (sendMedia) {
                    System.out.println("(TEST) Sending media message...");
                    File file = null;

                    try {
                        String mediaName = "test" + r.nextInt(2) + (r.nextBoolean() ? ".jpg" : ".png");
                        InputStream is = SSLChat.class.getResourceAsStream("/me/ericballard/sslchat/gui/resources/images/test/" + mediaName);

                        Files.copy(is, Paths.get(Media.path + mediaName), StandardCopyOption.REPLACE_EXISTING);
                        file = new File(Media.path + mediaName);
                    } catch (IOException e) {
                        continue;
                    }

                    String data = "MEDIA:" + userColor + ";" + app.username;

                    app.client.imgToSend = file;
                    app.client.dataToSend.add(data);
                } else {//if (seed < 25) {
                    // Text
                    System.out.println("(TEST) Sending text message...");

                    // Simulate typing
                    app.controller.typing = true;

                    int sleepTime = r.ints(500, 5000).findFirst().getAsInt();
                    System.out.println("(TEST) Sleeping while typing " + ((double) sleepTime / 1000.0) + "s");
                    Thread.sleep(sleepTime);

                    // Generate message
                    String msg = null;
                    int wordCount = r.ints(1, 20).findFirst().getAsInt();

                    for (int words = 0; words < wordCount; words++) {
                        int charCount = r.ints(1, 15).findFirst().getAsInt();
                        String word = null;

                        for (int i = 0; i < charCount; i++) {
                            char c = (char) (r.nextInt(26) + 'a');
                            word = (word != null ? word : "") + c;
                        }

                        msg = (msg != null ? msg + " " : "") + word;
                    }

                    String data = userColor + ";" + app.username + ";" + msg;

                    app.controller.typing = false;
                    app.client.dataToSend.add("MESSAGE:" + data);
                    System.out.println("(TEST) Send Text: " + msg);
                }

                Thread.sleep(r.nextInt(60000));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("(ERROR) Due to: " + e.getMessage());
                break;
            }
        }

        // Close app and disconnect from server
        Platform.runLater(() -> app.controller.anchorPane.getScene().getWindow().hide());
        System.out.println("(GG) Successful");
    }
}
