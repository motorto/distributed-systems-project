package ds.assignment.gossiping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Peer
 */
public class Peer {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Missing Arguments: Peer my_ip my_port");
            System.exit(1);
        }

        new Thread(new Server(args[0], args[1])).start();
        new Thread(new Client()).start();
    }
}

class Server implements Runnable {
    ServerSocket server;
    static HashMap<String, Socket> neighbours;
    static String ip_addr;
    static String ip_port;

    public Server(String ip_addr, String ip_port) throws NumberFormatException, UnknownHostException, IOException {
        Server.ip_addr = ip_addr;
        Server.ip_port = ip_port;
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
        Server.neighbours = new HashMap<>();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket client = server.accept();
                new Thread(new Connection(client)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void register(String neigh_ip_addr, String neigh_ip_port, boolean is_acknowledgement)
            throws NumberFormatException, UnknownHostException, IOException {
        String new_node = neigh_ip_addr + ":" + neigh_ip_port;
        System.out.println("Trying to Register: " + new_node);

        if (!neighbours.containsKey(new_node)) {
            System.out.println("Added: " + new_node);
            Socket socket = new Socket(InetAddress.getByName(neigh_ip_addr), Integer.parseInt(neigh_ip_port));
            neighbours.put(new_node, socket);

            if (!is_acknowledgement) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("register " + "acknowledgement " + ip_addr + " " + ip_port);
                out.flush();
            }
        }
    }
}

class Connection implements Runnable {
    Socket socket;

    public Connection(Socket client) {
        this.socket = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner parser = new Scanner(in.readLine());
            switch (parser.next()) {
                case "register":
                    String is_acknowledgement = parser.next();
                    if (is_acknowledgement.equals("acknowledgement")) {
                        Server.register(parser.next(), parser.next(), true);
                    } else {
                        Server.register(is_acknowledgement, parser.next(), false);
                    }
                    break;
                default:
                    System.err.println("Invalid Call");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Command Line
 */
class Client implements Runnable {
    Scanner scanner;

    public Client() throws Exception {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            switch (scanner.next()) {
                case "register":
                    try {
                        Server.register(scanner.next(), scanner.next(), false);
                    } catch (NumberFormatException | IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    System.err.println("Invalidi Call");
                    break;
            }
        }
    }
}