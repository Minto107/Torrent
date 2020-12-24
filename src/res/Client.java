package res;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    Socket client;
    boolean readMode;
    BufferedReader bf;

    public Client(int port) {
        try {
            boolean exit = false;
            readMode = true;
            Scanner scanner = new Scanner(System.in);
            client = new Socket(InetAddress.getByName("localhost"), port);
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), false);
            bf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while (!exit) {
                readFromServer();
                Thread.sleep(1000);
                System.out.print("Enter file number that you'd like to download or write exit to exit: ");
                String choice = scanner.nextLine();
                System.out.println(choice);
                Pattern pattern = Pattern.compile("[C-Z]?:?[\\\\]*[\\wW.]*");
                Matcher matcher = pattern.matcher(choice);
                boolean match = matcher.find();
                if (choice.equals("exit")) {
                    exit = true;
                    client.close();
                } else if (isInteger(choice)) {
                    printWriter.write(choice + '\n');
                    System.out.println("Sending choice...");
                    printWriter.flush();
                    System.out.println("Sent!");
                    String name = receiveName();
                    System.out.println("Received name");
                    receiveFile(name);
                } else if(match){
                    System.out.println("Sending file... ");
                    printWriter.write(choice+'\n');
                    printWriter.flush();
                    sendName(choice);
                } else {
                    System.out.println("That's not what we wanted to see");
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Client closing...");
        }
    }

    private void readFromServer() {
        Thread thread = new Thread(()->{
            try {
                String userInput;
                int counter = 0;
                while (true) {
                    if (!readMode){
                        if (counter==0) {
                            //this.writer = new PrintWriter("D:\\Torrent\\test.cpp");
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
                ioException.printStackTrace();
            }
        });
        thread.start();
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
        String filePath = "D:\\Torrent\\" + name;
        FileOutputStream fos = new FileOutputStream(filePath);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();
        int reading;
        while((reading=is.read(file))!=-1)
            bos.write(file, 0, reading);
        bos.flush();
        socket.close();

        System.out.println("File " + name + " saved to " + filePath);
        readMode = true;
    }

    private void sendName(String filePath) throws IOException {
        ServerSocket nameSocket = new ServerSocket(5001);
        Pattern pattern = Pattern.compile("[\\\\]{0}[\\w-.]*$");
        Matcher matcher = pattern.matcher(filePath);
        matcher.find();
        String name = matcher.group(0);
        Socket nameS = nameSocket.accept();
        System.out.println("Connected!");
        OutputStream os = nameS.getOutputStream();
        os.write(name.getBytes());
        os.close();
        nameSocket.close();
        nameS.close();
        System.out.println("Name was sent!");
        sendFile(filePath);
    }

    private void sendFile(String filePath) throws IOException {
        ServerSocket ssDL = new ServerSocket(5000);
        System.out.println("Waiting for connection...");
        Socket socket = ssDL.accept();
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        OutputStream os = socket.getOutputStream();
        byte[] contents;
        long fileLength = file.length();
        long current = 0;
        System.out.println("Sending file ... ");
        while(current!=fileLength){
            int size = 10000;
            if(fileLength - current >= size)
                current += size;
            else{
                size = (int)(fileLength - current);
                current = fileLength;
            }
            contents = new byte[size];
            bis.read(contents, 0, size);
            os.write(contents);
            //System.out.print((current*100)/fileLength+"% ");
        }
        os.flush();
        //File transfer done. Close the socket connection!
        socket.close();
        ssDL.close();
        System.out.println("File sent succesfully!");
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
        new Client(44431);
    }
}
