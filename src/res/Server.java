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
            log("Server listening on port " + ss.getLocalPort());
            readFilesFromDirectory();
            retrieveFiles();
            log("Waiting for clients to connect...");
            /*server = ss.accept();
            handleClient(ss.accept());
            log("Client " + clientID + " connected!");
            sendFileListToClient();*/
            while (true) {
                handleClient(ss.accept());
                log("Client " + clientID + " connected!");
                /*BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream()));
                String userInput = reader.readLine();
                if (userInput != null) {
                    if (isInteger(userInput)) {
                        sendName(Integer.parseInt(userInput));
                    } else {
                        String name = receiveName();
                        System.out.println(name);
                        receiveFile(name);
                    }
                    //sendFileListToClient();
                }*/
            }
        } catch (IOException e) {
            log("Server is closing...");
        }
    }

    private void handleClient(Socket socket) {
        clientID++;
        AtomicBoolean alive = new AtomicBoolean(true);
        log("Creating handler for client " + clientID);
        new Thread(() -> {
            try {
                sendClientID();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                sendFileListToClient(socket);
                String userInput = reader.readLine();
                if (userInput != null) {
                    if (isInteger(userInput)) {
                        sendName(Integer.parseInt(userInput));
                    } else if (userInput.equals("e")) {
                        alive.set(false);
                        reader.close();
                        socket.close();
                        log("Client " + clientID + " disconnected.");
                    } else {
                        String name = receiveName();
                        System.out.println(name);
                        receiveFile(name);
                    }
                    if (alive.get())
                        sendFileListToClient(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendClientID() throws IOException {
        ServerSocket serverSocket = new ServerSocket(5010);
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

    /**
     * Sends the selected file to the client.
     *
     * @param index File number provided from client.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
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

    /**
     * Receives file from the client.
     *
     * @param name Name of file received from the client that will be used to save it as on the server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
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

    /**
     * Prints details about files that are stored on the file server.
     */
    private void retrieveFiles() {
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
            output.append('\n');
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
        log("Preparing file list...");
        torrentFiles = new ArrayList<>();
        String homeLocation = "D:\\TorrentS";
        File actual = new File("D:\\TorrentS");
        for (File f : Objects.requireNonNull(actual.listFiles())) {
            torrentFiles.add(new TorrentFile(homeLocation + "\\" + f.getName()));
        }
        log("File list is ready.");
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
