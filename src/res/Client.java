package res;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    Socket client;
    boolean readMode;
    BufferedReader bf;

    public Client(int port) {
        try {
            boolean exit = false;
            readMode = true;
            Scanner scanner = new Scanner(System.in);
            client = new Socket("localhost", port);
            PrintWriter writer = new PrintWriter(client.getOutputStream(), false);
            bf = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while (!exit) {
                Thread read = new Thread(() -> {
                    if (readMode) {

                    }
                });
                read.start();
                System.out.print("Enter file number that you'd like to download or write exit to exit: ");
                String choice = scanner.nextLine();
                System.out.println(choice);
                if (choice.equals("exit")) {
                    exit = true;
                    client.close();
                } else if (isInteger(choice)) {
                    StringBuilder sb = new StringBuilder(choice);
                    sb.append('\n');
                    writer.write(sb.toString());
                    writer.flush();
                    InputStream is = client.getInputStream();
                    FileOutputStream fos = new FileOutputStream("D:\\Torrent\\test.cpp");
                    byte[] receive = new byte[2002];
                    is.read(receive, 0, receive.length);
                    fos.write(receive, 0, receive.length);
                    //receiveFile("D:\\Torrent\\test.cpp");
                } else {
                    System.out.println("Thats not what we wanted to see");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*private void receiveFile(String fileLocation) {
        try {
            InputStream is = client.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileLocation);
            byte[] receive = new byte[2002];
            char[] receiveffs = new char[receive.length];
            is.read(receive, 0, receive.length);
            fos.write(receive, 0, receive.length);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }*/

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
