import res.Client;
import res.Server;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-S"))
                new Server(44431, false);
            if (args[0].equals("-SMH"))
                new Server(44431, true);
            if (args[0].equals("-C"))
                new Client(44431, false);
            if (args[0].equals("-CMH"))
                new Client(44431, true);

        } else {
            System.out.println("You have to specify how you launch the program. ");
        }
    }
}
