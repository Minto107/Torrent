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
                    StringBuilder sb = new StringBuilder(choice);
                    sb.append('\n');
                    printWriter.write(sb.toString());
                    printWriter.flush();
                    String name = receiveName();
                    receiveFile(name);
                    //InputStream is = client.getInputStream();
                    //FileOutputStream fos = new FileOutputStream("D:\\Torrent\\test.cpp");
                    //fos.write(receive, 0, receive.length);
                    //receiveFile("D:\\Torrent\\test.cpp");
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
                while (((userInput = bf.readLine()) != null)) {
                    if (!readMode){
                        if (counter==0) {
                            //this.writer = new PrintWriter("D:\\Torrent\\test.cpp");
                            System.out.println("Reading will stop");
                            counter++;
                        }
                        /*    writer.write(userInput + '\n');
                            writer.flush();

                        System.out.println(userInput);*//*
                        byte[] contents = new byte[10000];

                        //Initialize the FileOutputStream to the output file's full path.
                        FileOutputStream fos = new FileOutputStream("D:\\Torrent\\test.cpp");
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        InputStream is = client.getInputStream();

                        //No of bytes read in one read() call
                        int bytesRead = 0;

                        while((bytesRead=is.read(contents))!=-1)
                            bos.write(contents, 0, bytesRead);

                        bos.flush();
                        client.close();

                        System.out.println("File saved successfully!");*/
                    } else {
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

    /*private void receiveFile(String fileLocation) {
        try {
            InputStream is = client.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileLocation);
            byte[] receive = new byte[2002];
            is.read(receive, 0, receive.length);
            fos.write(receive, 0, receive.length);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }*/

    private String receiveName() throws IOException {
        Socket socket = new Socket(InetAddress.getByName("localhost"), 5001);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String name = reader.readLine();
        System.out.println(name);
        reader.close();
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
        int bytesRead = 0;

        while((bytesRead=is.read(contents))!=-1)
            bos.write(contents, 0, bytesRead);

        bos.flush();
        socket.close();

        System.out.println("File saved successfully!");
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
