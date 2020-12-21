package res;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    List<File> files;
    ServerSocket ss;
    Socket server;
    BufferedReader reader;

    public Server(int port) {
        try {
            ss = new ServerSocket(port);
            System.out.println("Server listening on port " + ss.getLocalPort());
            files = new ArrayList<>();
            files.add(new File("GPU-Z", "C:\\Users\\minto.MSI-B450TM\\Downloads\\GPU-Z.2.36.0.exe"));
            files.add(new File("C:\\Users\\minto.MSI-B450TM\\Downloads\\main.cpp"));
            retrieveFiles();
            System.out.println("Waiting for clients to connect...");
            server = ss.accept();
            System.out.println("Client connected!");
            sendFileListToClient();
            while (true) {
                reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
                String userInput = reader.readLine();
                if (userInput != null) {
                    System.out.println(userInput);
                    sendFile(Integer.parseInt(userInput));
                }
            }
        } catch (IOException e) {
            System.err.println("Nobody cares");
        }
    }

    private void sendFile(int fileIndex) {
        fileIndex -= 1;
        byte[] array = new byte[2000];
        try {
            FileInputStream fr = new FileInputStream(files.get(fileIndex).fileLocation);
            System.out.println("Preparing to send " + files.get(fileIndex).name + "...");
            fr.read(array, 0, array.length);
            OutputStream os = server.getOutputStream();
            os.write(array, 0, array.length);
            System.out.println(files.get(fileIndex).name + " was sent!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retrieveFiles() {
        System.out.println("There are " + files.size() + " files on the server." +
                "\nFull list:");
        for (int i = 0; i < files.size(); i++) {
            System.out.println("#" + (i + 1) + ": " + (files.get(i).toString()));
        }
    }

    private void sendFileListToClient() {
        try {
            StringBuilder output = new StringBuilder();
            server.getOutputStream().write(("There are " + files.size() + " files on the server." +
                    "\nFull list:").getBytes());
            for (int i = 0; i < files.size(); i++) {
                output.append("#").append(i + 1).append(": ").append(files.get(i).toString());
                output.append('\n');
            }
            output.append('\n');
            String toSend = output.toString();
            server.getOutputStream().write((toSend).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Server(44431);
    }
}
