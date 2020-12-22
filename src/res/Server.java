package res;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    List<TorrentFile> torrentFiles;
    ServerSocket ss;
    Socket server;
    BufferedReader reader;

    public Server(int port) {
        try {
            ss = new ServerSocket(port);
            System.out.println("Server listening on port " + ss.getLocalPort());
            torrentFiles = new ArrayList<>();
            torrentFiles.add(new TorrentFile("C:\\Users\\minto.MSI-B450TM\\Downloads\\GPU-Z.2.36.0.exe"));
            torrentFiles.add(new TorrentFile("C:\\Users\\minto.MSI-B450TM\\Downloads\\main.cpp"));
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
                    sendName(Integer.parseInt(userInput));
                    }
            }
        } catch (IOException e) {
            System.out.println("Server is closing...");
        }
    }

    /*private void sendFile(int fileIndex) {
        fileIndex -= 1;
        byte[] array = new byte[files.get(fileIndex).fileLocation.length()];
        try {
            FileInputStream fr = new FileInputStream(files.get(fileIndex).fileLocation);
            System.out.println("Preparing to send " + files.get(fileIndex).name + "...");
            System.out.println(fr.read(array, 0, array.length));
            OutputStream os = server.getOutputStream();
            os.write(array, 0, array.length);
            System.out.println(files.get(fileIndex).name + " was sent!");
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/

    private void sendName(int index) throws IOException {
        index -=1;
        ServerSocket nameSocket = new ServerSocket(5001);
        Socket nameS = nameSocket.accept();
        OutputStream os = nameS.getOutputStream();
        os.write(torrentFiles.get(index).name.getBytes());
        os.close();
        sendFileNew(index+1);
    }

    private void sendFileNew(int index) throws IOException {
        //Initialize Sockets
        ServerSocket ssDL = new ServerSocket(5000);
        System.out.println("Waiting for connection...");
        Socket socket = ssDL.accept();
        index -= 1;
        //Specify the file
        File file = new File(torrentFiles.get(index).fileLocation);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);

        //Get socket's output stream
        OutputStream os = socket.getOutputStream();

        //Read File Contents into contents array
        byte[] contents;
        long fileLength = file.length();
        long current = 0;
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
            System.out.print("Sending file ... "+(current*100)/fileLength+"% complete!");
        }

        os.flush();
        //File transfer done. Close the socket connection!
        socket.close();
        ssDL.close();
        System.out.println("File sent succesfully!");
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

    public static void main(String[] args) {
        new Server(44431);
    }
}
