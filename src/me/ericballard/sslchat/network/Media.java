package me.ericballard.sslchat.network;

import javax.net.ssl.SSLSocket;
import javax.swing.*;
import java.io.*;

public class Media {

    static final String path = new JFileChooser().getFileSystemView().getDefaultDirectory().toString() + File.separator + "SSLChat" + File.separator;

    public static File receive(SSLSocket socket) throws IOException {
        InputStream in = socket.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(in);
        DataInputStream dis = new DataInputStream(bis);

        // Receive # of byte in file
        long fileLength = dis.readLong();

        // Receive file name
        String fileName = dis.readUTF();

        System.out.println("FILE INFO (" + fileName + " | " + (fileLength / 1000) + "kb)");

        // Write bytes to file
        File folder = new File(path);

        if (!folder.exists())
            folder.mkdirs();

        File file = new File(path + fileName);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        for (int curByte = 0; curByte < fileLength; curByte++) {
            bos.write(bis.read());
        }

        bos.close();
        return file;
    }

    public static void send(SSLSocket socket, File file) throws IOException {
        OutputStream os = socket.getOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(os);
        DataOutputStream dos = new DataOutputStream(bos);

        // Send server size of file
        long length = file.length();
        dos.writeLong(length);

        // Send server name of file
        String name = file.getName();
        dos.writeUTF(name);

        // Send file
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        int curByte = 0;
        while ((curByte = bis.read()) != -1) {
            bos.write(curByte);
        }

        bis.close();
        dos.close();
    }

}
