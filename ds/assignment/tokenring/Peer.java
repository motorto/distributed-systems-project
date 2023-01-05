package ds.assignment.tokenring;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Scanner;

public class Peer {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Missing Arguments: Peer my_ip my_port next_node_ip next_node_port");
            System.exit(1);
        }

        new Thread(new Server(args[0], args[1], args[2], Integer.parseInt(args[3]))).start();
        new Thread(new Client()).start();
    }
}

class Server implements Runnable {
    ServerSocket server;
    static String next_node_ip, node_ip, node_port;
    static int next_node_port, token = 0;
    static boolean lock = false;

    public Server(String node, String node_port, String next_node_ip, int next_node_port) throws Exception {
        Server.next_node_ip = next_node_ip;
        Server.next_node_port = next_node_port;
        Server.node_ip = node;
        Server.node_port = node_port;
        this.server = new ServerSocket(Integer.parseInt(node_port), 1, InetAddress.getByName(node));
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

    public static void lock() {
        Server.lock = true;
    }

    public static void unlock() {
        try {

            Socket socket = new Socket(InetAddress.getByName(next_node_ip), next_node_port);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(String.valueOf(Server.token));
            out.flush();

            socket.close();

            Server.lock = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Connection implements Runnable {
    Socket socket;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner parser = new Scanner(in.readLine());
            Server.token = Integer.parseInt(parser.next()) + 1;
            System.out.println(Server.node_ip + ":" + Server.node_port + " " + Server.token);
            parser.close();
            socket.close();

            if (!Server.lock) {
                Server.unlock();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Client implements Runnable {
    Scanner scanner;

    public Client() throws Exception {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            switch (scanner.nextLine()) {
                case "lock":
                    Server.lock();
                    break;
                case "unlock":
                    Server.unlock();
                    break;
                default:
                    System.err.println("Invalid Call");
                    break;
            }
        }
    }
}
