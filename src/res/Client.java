package res;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
    private boolean readMode = true;
    private BufferedReader bf;
    private int clientID;
    private String saveLocation;
    private static boolean multiHostMode;

    /**
     * Creates Client object and connects with a file server.
     * @param port Port number that client will try to connect to.
     */
    public Client(int port) {
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
                    receiveFile(name);
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
            e.printStackTrace();
        }
    }

    private void readClientID() throws IOException {
        Socket receive = new Socket(InetAddress.getByName("localhost"), 5010);
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
        Thread thread = new Thread(() -> {
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
        });
        thread.start();
    }

    /**
     * Receives file name that will be later used to create and store a file.
     * @return Returns String containing file name that will be used to store a file.
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
     * Receives the file from the server.
     * @param name String that will be used as file name to store it in download location.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void receiveFile(String name) throws IOException {
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5000);
        byte[] file = new byte[10000];
        String filePath = saveLocation + "\\" + name;
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        int reading;
        while ((reading = is.read(file)) != -1)
            bos.write(file, 0, reading);
        bos.flush();
        bos.close();
        fos.close();
        socket.close();
        System.out.println("File " + name + " saved to " + filePath);
        readMode = true;
    }

    /**
     * Sends the file name to the server.
     * @param filePath String containing path to the file that's name will be sent to the file server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendName(String filePath) throws IOException {
        ServerSocket nameSocket = new ServerSocket(5001);
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
     * @param filePath String containing path to the file that will be sent to the file server.
     * @throws IOException if an I/O error occurs when opening the socket.
     */
    private void sendFile(String filePath) throws IOException {
        ServerSocket ssDL = new ServerSocket(5000);
        System.out.println("Waiting for connection...");
        Socket socket = ssDL.accept();
        File file = new File(filePath);
        if (file.exists()){
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

    /**
     * Checks if received line from client is an Integer.
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
     * Runs the client.
     * @param args You can provide additional parameter (-MH) to run client in multihost mode.
     */
    public static void main(String[] args) {
        if (args != null || args[0].equals("-MH"))
            multiHostMode = true;
        new Client(44431);
    }
}
