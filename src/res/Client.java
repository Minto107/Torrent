package res;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client class creates a client that connects to desired server on the same computer(localhost).
 */
public class Client {
    private boolean run = false;
    private boolean exit = false;
    private Socket client;
    private final boolean multiHostMode;
    private BufferedReader bf;
    private boolean readMode = true, ok;
    private String saveLocation;
    private int clientID, partID = 0;

    /**
     * Creates Client object and connects with a file server.
     *
     * @param port Port number that client will try to connect to.
     */
    public Client(int port, boolean multiHostMode) {
        this.multiHostMode = multiHostMode;
        try {
            Scanner scanner = new Scanner(System.in);
            client = new Socket(InetAddress.getByName("localhost"), port);
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), false);
            bf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while (!exit) {
                if (!run) {
                    readClientID();
                    System.out.println("Your client ID: " + clientID);
                }
                readFromServer();
                Thread.sleep(1000);
                System.out.print("Enter file number that you'd like to download or write exit to exit: ");
                String choice = scanner.nextLine();
                System.out.println(choice);
                Pattern pattern = Pattern.compile("[C-Z]?:?[\\\\]*[\\wW.]*");
                Matcher matcher = pattern.matcher(choice);
                boolean match = matcher.find();
                if (choice.equals("exit") || choice.equals("e")) {
                    exit = true;
                    printWriter.write("e\n");
                    printWriter.flush();
                    printWriter.close();
                    readMode = false;
                    bf.close();
                    scanner.close();
                    client.close();
                    System.out.println("Client " + clientID + " has successfully been closed.");
                } else if (isInteger(choice)) {
                    printWriter.write(choice + '\n');
                    System.out.println("Sending choice...");
                    printWriter.flush();
                    System.out.println("Sent!");
                    String name = receiveName();
                    System.out.println("Received name");
                    if (!multiHostMode) {
                        receiveFile(name);
                    } else {
                        receiveFile(name + ".001");
                        receiveFile(name + ".002");
                        receiveFile(name + ".003");
                        ok = true;
                        receiveFile(name + ".004");
                        ok = false;
                        List<File> files = new ArrayList<>();
                        files.add(new File(saveLocation + "\\" + name + ".001"));
                        files.add(new File(saveLocation + "\\" + name + ".002"));
                        files.add(new File(saveLocation + "\\" + name + ".003"));
                        File file = new File(saveLocation + "\\" + name + ".004");
                        if (file.exists())
                            files.add(file);
                        mergeFiles(files, new File(saveLocation + "\\" + name));
                        for (File i : files) {
                            i.delete();
                        }
                        partID = 0;
                    }
                } else if (match) {
                    System.out.println("Sending file... ");
                    printWriter.write(choice + '\n');
                    printWriter.flush();
                    sendName(choice);
                } else {
                    System.out.println("You have provided wrong file number or wrong path to file to send to the server.");
                }
            }
        } catch (IOException | InterruptedException e) {
            if (!ok) {
                e.printStackTrace();
            }
        }
    }

    public static void mergeFiles(List<File> files, File target)
            throws IOException {
        try (FileOutputStream fos = new FileOutputStream(target);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            for (File f : files) {
                Files.copy(f.toPath(), bos);
            }
        }
    }

    private void readClientID() throws IOException {
        Socket receive = new Socket(InetAddress.getByName("localhost"), 10001);
        BufferedReader reader = new BufferedReader(new InputStreamReader(receive.getInputStream()));
        clientID = Integer.parseInt(reader.readLine());
        reader.close();
        receive.close();
        run = true;
        setDownloadLocation();
    }

    private void setDownloadLocation() {
        saveLocation = "D:\\Torrent_" + clientID;
        new File(saveLocation).mkdir();
    }

    /**
     * Starts new thread that will read any messages that are being sent from the server.
     */
    private void readFromServer() {
        if (!run)
            System.out.println("Connected to a file server at " + client.getPort());
        new Thread(() -> {
            try {
                String userInput;
                int counter = 0;
                while (true) {
                    if (!readMode) {
                        if (counter == 0) {
                            System.out.println("Reading will stop");
                            counter++;
                        }
                    } else {
                        userInput = bf.readLine();
                        System.out.println(userInput);
                        System.out.println(bf.readLine());
                    }
                }
            } catch (IOException ioException) {
                if (!exit)
                    ioException.printStackTrace();
            }
        }).start();
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
     * Receives file name that will be later used to create and store a file.
     *
     * @return Returns String containing file name that will be used to store a file.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private String receiveName() throws IOException {
        System.out.println("Creating socket...");
        int port = 5001 + (clientID * 10);
        Socket socket = new Socket(InetAddress.getByName("localhost"), port);
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
     * Receives the file from the server.
     *
     * @param name String that will be used as file name to store it in download location.
     */
    private void receiveFile(String name) {
        int port = 5000 + (clientID * 10);
        String filePath = saveLocation + "\\" + name;
        if (!multiHostMode) {
            try {
                Socket socket = new Socket(InetAddress.getByName("localhost"), port);
                System.out.println("Listening on port " + socket.getPort());
                byte[] file = new byte[10000];
                FileOutputStream fos = new FileOutputStream(filePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                InputStream is = socket.getInputStream();
                int reading;
                while ((reading = is.read(file)) != -1)
                    bos.write(file, 0, reading);
                bos.flush();
                bos.close();
                fos.close();
                is.close();
                socket.close();
                System.out.println("File " + name + " saved to " + filePath);
            } catch (IOException e) {
                //
            }
        } else {
            try {
                Socket socket = new Socket(InetAddress.getByName("localhost"), port + partID);
                partID++;
                System.out.println("Listening on port " + socket.getPort());
                byte[] file = new byte[10000];
                FileOutputStream fos = new FileOutputStream(filePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                InputStream is = socket.getInputStream();
                int reading;
                while ((reading = is.read(file)) != -1)
                    bos.write(file, 0, reading);
                bos.flush();
                bos.close();
                fos.close();
                is.close();
                socket.close();
                System.out.println("File " + name + " saved to " + filePath);
            } catch (IOException e) {
                //
            }
        }
        readMode = true;
    }

    /**
     * Sends the file name to the server.
     *
     * @param filePath String containing path to the file that's name will be sent to the file server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendName(String filePath) throws IOException {
        int port = 5001 + (clientID * 10);
        ServerSocket nameSocket = new ServerSocket(port);
        Pattern pattern = Pattern.compile("[\\\\]{0}[\\w-.]*$");
        Matcher matcher = pattern.matcher(filePath);
        matcher.find();
        String name = matcher.group(0);
        Socket nameSoc = nameSocket.accept();
        System.out.println("Connected!");
        OutputStream os = nameSoc.getOutputStream();
        os.write(name.getBytes());
        os.close();
        nameSocket.close();
        nameSoc.close();
        System.out.println("Name was sent!");
        sendFile(filePath);
    }

    /**
     * Sends file to the server.
     *
     * @param filePath String containing path to the file that will be sent to the file server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendFile(String filePath) throws IOException {
        int port = 5000 + (clientID * 10);
        ServerSocket ssDL = new ServerSocket(port);
        System.out.println("Waiting for connection...");
        Socket socket = ssDL.accept();
        File file = new File(filePath);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            OutputStream os = socket.getOutputStream();
            byte[] contents;
            long fileLength = file.length();
            long current = 0;
            System.out.println("Sending file ... ");
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
            System.out.println("File sent successfully!");
        } else {
            System.err.println(filePath + " doesn't exists.");
            socket.close();
            ssDL.close();
        }
    }
}
