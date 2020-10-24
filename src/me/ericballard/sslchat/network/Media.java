package me.ericballard.sslchat.network;

import javafx.util.Pair;

import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Media {

    public static final String path = new JFileChooser().getFileSystemView().getDefaultDirectory().toString() + File.separator + "SSLChat" + File.separator;

    public static File receive(SSLSocket socket, BufferedReader reader) {
        try {
            // Receive # of byte in file
            int fileLength = (int) Long.parseLong(reader.readLine());

            // Receive file name
            String fileName = reader.readLine();

            // Write bytes to file
            byte[] bytes;

            File file = new File(path + fileName);

            // Read bytes from stream
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            bytes = (byte[]) ois.readObject();

            //Verify
            boolean corrupt = fileLength != bytes.length;
            System.out.println("Received file '" + file.getName() + "' - Corrupt: " + corrupt + " | Read Byte: " + bytes.length + "/" + fileLength + ")");

            if (corrupt) {
                file.delete();
                return null;
            }

            return Files.write(Paths.get(file.getPath()), bytes).toFile();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void send(SSLSocket socket, PrintWriter writer, File file) {
        try {
            // Send server size of file
            int length = (int) file.length();
            writer.println(length);

            // Send server name of file
            String name = file.getName();
            writer.println(name);

            System.out.println("Sending media: " + name + " | " + length + " bytes)");

            // Convert file to byte[]
            byte[] bytes = new byte[length];
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(bytes, 0, bytes.length);

            // Write bytes to stream
            ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));

            oos.writeObject(bytes);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Resize image w/h to be < 150px - by ratio
    public static Pair<Double, Double> resize(double width, double height) {
        if (width > 150 || height > 150) {
            for (double multiplier = 0.1; multiplier < 1.0; multiplier += 0.1) {
                double w = width - (width * multiplier);
                double h = height - (height * multiplier);

                if (w < 150 && h < 150) {
                    System.out.println("Media size reduced by " + multiplier + "%");

                    if (multiplier >= 0.99)
                        return new Pair(150D, 150D);
                    else
                        return new Pair<>(w, h);
                }
            }
        }

        return new Pair<>(width, height);
    }
}
