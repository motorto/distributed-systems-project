package ds.assignment.multicast;

import java.io.BufferedReader;
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
    static boolean i_offer_service = false;
    static HashSet<String> network = new HashSet<>();
    static PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>();

    public static void main(String[] args) throws IOException {
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
        if (!i_offer_service) {
            new Thread(new Client()).start();
        }
    }
}

class Server implements Runnable {
    ServerSocket server;
    static String ip_port, ip_addr;

    public Server(String ip_addr, String ip_port) throws NumberFormatException, UnknownHostException, IOException {
        Server.ip_port = ip_port;
        Server.ip_addr = ip_addr;
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
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

    public static void send_message(int flag, Message to_send)
            throws NumberFormatException, UnknownHostException, IOException {

        if (flag == 0) {
            to_send.setType_of_message("message");
        } else if (flag == 1) {
            to_send.setType_of_message("ack");
        } else {
            System.err.println("You found a bug at send_message");
            System.exit(-1);
        }

        to_send.setIp_addr_origin(Server.ip_addr);
        to_send.setIp_port_origin(Server.ip_port);

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

    public static void update_clocks(Message message_received) {
        long old_value = Peer.timestamp.get();
        while (true) {
            if (Peer.timestamp.compareAndSet(old_value, Math.max(old_value, message_received.getTimestamp() + 1))) {
                break;
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
            Message message_received = new Message().parse(parser.nextLine());

            // DEBUG
            System.out.println(message_received);
            // End of DEBUG

            Server.update_clocks(message_received);
            System.out.println("ab");

            Peer.queue.add(message_received); // TODO: The issue is here commenting line 147 it appears on all queues
                                              // ...

            switch (message_received.getType_of_message()) {
                case "message":
                    Server.send_message(1, message_received);
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
