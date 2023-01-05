package ds.assignment.multicast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Peer {
    static final AtomicLong timestamp = new AtomicLong();
    static HashSet<String> network = new HashSet<>();
    static PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 3) {
            System.err.println("Missing Arguments: Peer my_ip my_port file_with_ip_of_all_networks");
            System.exit(1);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(args[2]))) {
            String line;
            String my_ip = args[0] + ":" + args[1];
            while ((line = br.readLine()) != null) {
                if (line.substring(0, 2).equals("s:")) {
                    String line_tmp = line.replace("s:", "");
                    if (line_tmp.equals(my_ip)) {
                        // Peer.i_offer_service = true; TODO: CHANGE THIS BACK, JUST DEBUGGING
                        continue;
                    }
                    Peer.network.add(line_tmp);
                } else {
                    if (line.equals(my_ip)) {
                        continue;
                    }
                    Peer.network.add(line);
                }
            }
        }

        new Thread(new Server(args[0], args[1])).start();
        new Thread(new Shell()).start();
    }
}

class Server implements Runnable {
    ServerSocket server;
    static String ip, port;

    public Server(String ip, String port) throws NumberFormatException, UnknownHostException, IOException {
        Server.ip = ip;
        Server.port = port;
        this.server = new ServerSocket(Integer.parseInt(port), 1, InetAddress.getByName(ip));
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

    public static void update_clocks(Message message_received) {
        long old_value = Peer.timestamp.get();
        while (true) {
            if (Peer.timestamp.compareAndSet(old_value, Math.max(old_value, message_received.getTimestamp() + 1))) {
                break;
            }
        }
    }

    public static void send_message(Message to_send)
            throws NumberFormatException, UnknownHostException, IOException {

        Peer.queue.add(to_send);

        for (String ip_tmp : Peer.network) {
            String ip_to_create_connection[] = ip_tmp.split(":");

            Socket socket = new Socket(InetAddress.getByName(ip_to_create_connection[0]),
                    Integer.parseInt(ip_to_create_connection[1]));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(to_send);
            out.flush();
            out.close();
            socket.close();
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
            Message message_received = new Message().parse(parser.nextLine());

            Server.update_clocks(message_received);
            Peer.queue.add(message_received);

            // DEBUG
            System.out.println(message_received);
            // End of DEBUG

            switch (message_received.getType_of_message()) {
                case "message":
                    Message to_ack = new Message(Peer.timestamp.getAndIncrement(), message_received.getMessage(),"ack");
                    Server.send_message(to_ack);
                    break;
                case "ack":
                    break;
                default:
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Shell implements Runnable {

    private Scanner scanner;

    public Shell() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            try {
                String input = scanner.nextLine();

                // DEBUG
                if (input.equalsIgnoreCase("queue")) {
                    System.out.println("DEBUG");
                    System.out.println("---QUEUE---");
                    for (Message m : Peer.queue) {
                        System.out.println(m);
                    }
                    System.out.println("-----");
                    continue;
                }
                // End DEBUG

                Message to_send = new Message(Peer.timestamp.getAndIncrement(), input, "message");
                Server.send_message(to_send);
            } catch (NumberFormatException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}