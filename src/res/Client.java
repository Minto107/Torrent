package res;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    Socket client;
    boolean readMode;
    BufferedReader bf;
    PrintWriter writer;

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

        //Initialize socket
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5000);
        byte[] contents = new byte[10000];

        //Initialize the FileOutputStream to the output file's full path.
        FileOutputStream fos = new FileOutputStream("D:\\Torrent\\" + name);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        InputStream is = socket.getInputStream();

        //No of bytes read in one read() call
        int reading = 0;

        while((reading=is.read(contents))!=-1)
            bos.write(contents, 0, reading);

        bos.flush();
        socket.close();

        System.out.println("File saved successfully!");
        readMode = true;
    }

    public static void main(String[] args) {
        new Client(44431);
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
