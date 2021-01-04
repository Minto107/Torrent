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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server class is used to create a file server
 */
public class Server {
    private List<TorrentFile> torrentFiles;
    private boolean firstRun;
    private static boolean multiHostMode;
    private int clientID = 0;

    /**
     * Creates new Server object
     *
     * @param port Enter port that you want the server to run on
     */
    public Server(int port) {
        try {
            firstRun = true;
            ServerSocket ss = new ServerSocket(port);
            if (multiHostMode)
                log("EXPERIMENTAL: Running server in Multihost mode!");
            log("Server listening on port " + ss.getLocalPort());
            readFilesFromDirectory();
            showFileList();
            log("Waiting for clients to connect...");
            while (true) {
                handleClient(ss.accept());
                log("Client " + clientID + " connected!");
            }
        } catch (IOException e) {
            log("Server is closing...");
        }
    }

    private void handleClient(Socket socket) {
        clientID++;
        int myID = clientID;
        AtomicBoolean alive = new AtomicBoolean(true);
        log("Creating handler for client " + myID);
        new Thread(() -> {
            try {
                sendClientID(myID);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendFileListToClient(socket);
                String userInput = reader.readLine();
                if (userInput != null) {
                    if (isInteger(userInput)) {
                        sendName(Integer.parseInt(userInput), myID);
                    } else if (userInput.equals("e")) {
                        alive.set(false);
                        reader.close();
                        socket.close();
                        log("Client " + myID + " disconnected.");
                    } else {
                        String name = receiveName(myID);
                        System.out.println(name);
                        receiveFile(name, myID);
                    }
                    if (alive.get())
                        sendFileListToClient(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendClientID(int clientID) throws IOException {
        ServerSocket serverSocket = new ServerSocket(10001);
        Socket send = serverSocket.accept();
        OutputStream os = send.getOutputStream();
        os.write(String.valueOf(clientID).getBytes());
        os.close();
        serverSocket.close();
        send.close();
    }

    /**
     * Sends name of the file to the client so it can download the file and use it's correct name and extension.
     *
     * @param index File number provided from client.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendName(int index, int clientID) throws IOException {
        index -= 1;
        int port = 5001 + (clientID * 10);
        ServerSocket nameSocket = new ServerSocket(port);
        Socket nameS = nameSocket.accept();
        OutputStream os = nameS.getOutputStream();
        os.write(torrentFiles.get(index).name.getBytes());
        os.close();
        nameSocket.close();
        nameS.close();
        sendFile(index + 1, clientID);
    }

    /**
     * Sends the selected file to the client.
     *
     * @param index File number provided from client.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendFile(int index, int clientID) throws IOException {
        int port = 5000 + (clientID * 10);
        ServerSocket ssDL = new ServerSocket(port);
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
        log("Sending file to client " + clientID + "...");
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
        os.close();
        bis.close();
        fis.close();
        socket.close();
        ssDL.close();
        log("File sent successfully!");
    }

    /**
     * Receives the file name from the client.
     *
     * @return Returns file name received from the client.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private String receiveName(int clientID) throws IOException {
        int port = 5001 + (clientID * 10);
        Socket socket = new Socket(InetAddress.getByName("localhost"), port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String name = reader.readLine();
        reader.close();
        socket.close();
        return name;
    }

    /**
     * Receives file from the client.
     *
     * @param name Name of file received from the client that will be used to save it as on the server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void receiveFile(String name, int clientID) throws IOException {
        int port = 5000 + (clientID * 10);
        Socket socket = new Socket(InetAddress.getByName("localhost"), port);
        byte[] file = new byte[10000];
        String filePath = "D:\\TorrentS\\" + name;
        log("Receiving file from client " + clientID + "...");
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        int reader;
        while ((reader = is.read(file)) != -1)
            bos.write(file, 0, reader);
        bos.flush();
        socket.close();
        torrentFiles.add(new TorrentFile(filePath));
        log("File " + name + " saved to " + filePath);
    }

    /**
     * Prints details about files that are stored on the file server.
     */
    private void showFileList() {
        System.out.println("There are " + torrentFiles.size() + " files on the server." +
                "\nFull list:");
        for (int i = 0; i < torrentFiles.size(); i++) {
            System.out.println("#" + (i + 1) + ": " + (torrentFiles.get(i).toString()));
        }
    }

    /**
     * Sends file list to the connected client(s).
     */
    private void sendFileListToClient(Socket socket) {
        try {
            StringBuilder output = new StringBuilder();
            socket.getOutputStream().write(("There are " + torrentFiles.size() + " files on the server." +
                    "\nFull list:\n").getBytes());
            for (int i = 0; i < torrentFiles.size(); i++) {
                output.append("#").append(i + 1).append(": ").append(torrentFiles.get(i).toString());
                output.append('\n');
            }
            String toSend = output.toString();
            socket.getOutputStream().write((toSend).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Provides basic logging functionality
     *
     * @param s String to save to the log and display on the console.
     */
    private void log(String s) {
        try {
            if (firstRun) {
                PrintWriter writer = new PrintWriter("log.txt");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                Date date = new Date(System.currentTimeMillis());
                writer.write("TORrent SKJ project by s21300\n");
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

    /**
     * Reads file to the array from default location.
     */
    private void readFilesFromDirectory() {
        long start = System.currentTimeMillis();
        log("Preparing file list...");
        torrentFiles = new ArrayList<>();
        String homeLocation = "D:\\TorrentS";
        File actual = new File("D:\\TorrentS");
        for (File f : Objects.requireNonNull(actual.listFiles())) {
            torrentFiles.add(new TorrentFile(homeLocation + "\\" + f.getName()));
        }
        long end = System.currentTimeMillis();
        log("Reading from directory took " + (end - start) + " ms.");
    }

    /**
     * Checks if received line from client is an Integer.
     *
     * @param s String to check if is an Integer.
     * @return Returns whether provided String is an Integer.
     */
    private static boolean isInteger(String s) {
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

    /**
     * Runs the server.
     *
     * @param args You can provide additional parameter (-MH) to run server in multihost mode.
     */
    public static void main(String[] args) {
        if (args != null || args[0].equals("-MH"))
            multiHostMode = true;
        new Server(44431);
    }
}
