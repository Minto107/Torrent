package res;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Server {
    List<TorrentFile> torrentFiles;
    ServerSocket ss;
    Socket server;
    BufferedReader reader;
    boolean firstRun;
    static boolean multiHostMode;

    public Server(int port) {
        try {
            readFilesFromDirectory();
            firstRun = true;
            ss = new ServerSocket(port);
            log("Server listening on port " + ss.getLocalPort());
            retrieveFiles();
            log("Waiting for clients to connect...");
            server = ss.accept();
            log("Client connected!");
            sendFileListToClient();
            while (true) {
                reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
                String userInput = reader.readLine();
                if (userInput != null) {
                    if (isInteger(userInput)) {
                        sendName(Integer.parseInt(userInput));
                    } else {
                        System.out.println("Preparing to receive the file....");
                        String name = receiveName();
                        System.out.println(name);
                        receiveFile(name);
                    }
                    sendFileListToClient();
                }
            }
        } catch (IOException e) {
            log("Server is closing...");
        }
    }

    private void sendName(int index) throws IOException {
        index -= 1;
        ServerSocket nameSocket = new ServerSocket(5001);
        Socket nameS = nameSocket.accept();
        OutputStream os = nameS.getOutputStream();
        os.write(torrentFiles.get(index).name.getBytes());
        os.close();
        nameSocket.close();
        nameS.close();
        sendFile(index + 1);
    }

    private void sendFile(int index) throws IOException {
        ServerSocket ssDL = new ServerSocket(5000);
        log("Waiting for connection...");
        Socket socket = ssDL.accept();
        index -= 1;
        File file = new File(torrentFiles.get(index).fileLocation);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        OutputStream os = socket.getOutputStream();
        byte[] contents;
        long fileLength = file.length();
        long current = 0;
        log("Sending file ... ");
        while (current != fileLength) {
            int size = 10000;
            if (fileLength - current >= size)
                current += size;
            else {
                size = (int) (fileLength - current);
                current = fileLength;
            }
            contents = new byte[size];
            bis.read(contents, 0, size);
            os.write(contents);
        }
        os.flush();
        socket.close();
        ssDL.close();
        log("File sent succesfully!");
    }

    private String receiveName() throws IOException {
        System.out.println("Creating socket...");
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5001);
        System.out.println("Socket created!");
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("Reading name...");
        String name = reader.readLine();
        System.out.println(name);
        reader.close();
        socket.close();
        return name;
    }

    private void receiveFile(String name) throws IOException {
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5000);
        byte[] file = new byte[10000];
        String filePath = "D:\\TorrentS\\" + name;
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        int reading;
        while ((reading = is.read(file)) != -1)
            bos.write(file, 0, reading);
        bos.flush();
        socket.close();
        torrentFiles.add(new TorrentFile(filePath));
        log("File " + name + " saved to " + filePath);
    }

    private void retrieveFiles() {
        System.out.println("There are " + torrentFiles.size() + " files on the server." +
                "\nFull list:");
        for (int i = 0; i < torrentFiles.size(); i++) {
            System.out.println("#" + (i + 1) + ": " + (torrentFiles.get(i).toString()));
        }
    }

    private void sendFileListToClient() {
        try {
            StringBuilder output = new StringBuilder();
            server.getOutputStream().write(("There are " + torrentFiles.size() + " files on the server." +
                    "\nFull list:\n").getBytes());
            for (int i = 0; i < torrentFiles.size(); i++) {
                output.append("#").append(i + 1).append(": ").append(torrentFiles.get(i).toString());
                output.append('\n');
            }
            output.append('\n');
            String toSend = output.toString();
            server.getOutputStream().write((toSend).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String s) {
        try {
            if (firstRun) {
                PrintWriter writer = new PrintWriter("log.txt");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                Date date = new Date(System.currentTimeMillis());
                writer.write("Logging started on " + formatter.format(date) + '\n');
                writer.flush();
                writer.close();
                Files.write(Paths.get("log.txt"), (s + '\n').getBytes(), StandardOpenOption.APPEND);
                firstRun = false;
            } else {
                Files.write(Paths.get("log.txt"), (s + '\n').getBytes(), StandardOpenOption.APPEND);
            }
            System.out.println("LOG: " + s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFilesFromDirectory() {
        torrentFiles = new ArrayList<>();
        String homeLocation = "D:\\TorrentS";
        File actual = new File("D:\\TorrentS");
        for (File f : Objects.requireNonNull(actual.listFiles())) {
            torrentFiles.add(new TorrentFile(homeLocation + "\\" + f.getName()));
        }
    }

    public static boolean isInteger(String s) {
        if (s == null) {
            return false;
        }
        int length = s.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (s.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        /*if (args[0].equals("-MH"))
            multiHostMode = true;*/
        new Server(44431);
    }
}
