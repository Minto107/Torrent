import res.Client;
import res.Server;

public class Main {
    /**
     * Handles launch flags and starts desired application.
     *
     * @param args Launch parameters, should be only one.
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            int port = 59999;
            if (args[0].equals("-S"))
                new Server(port, false);
            if (args[0].equals("-SMH"))
                new Server(port, true);
            if (args[0].equals("-C"))
                new Client(port, false);
            if (args[0].equals("-CMH"))
                new Client(port, true);
        } else {
            System.out.println("You have to specify how you launch the program. ");
        }
    }
}
