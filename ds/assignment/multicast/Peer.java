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
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class Peer {
    static final AtomicLong timestamp = new AtomicLong();
    static HashMap<String, Socket> neighbours = new HashMap<>();
    // static HashMap<String, Socket> servers_that_offer_service = new HashMap<>();
    static boolean i_am_a_server = false;

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
                        Peer.i_am_a_server = true;
                        continue;
                    }
                    Peer.neighbours.put(line_tmp, null);
                } else {
                    if (line.equals(my_ip)) {
                        continue;
                    }
                    Peer.neighbours.put(line, null);
                }
            }
        }

        new Thread(new Server(args[0], args[1])).start();

        if (!Peer.i_am_a_server)
            new Thread(new Client()).start();

    }
}

class Server implements Runnable {
    ServerSocket server;
    String ip_port, ip_addr;
    static PriorityQueue<Message> queue;

    public Server(String ip_addr, String ip_port) throws NumberFormatException, UnknownHostException, IOException {
        this.ip_port = ip_port;
        this.ip_addr = ip_addr;
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
        Server.queue = new PriorityQueue<>();
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

    private static void build_network() throws NumberFormatException, UnknownHostException, IOException {
        for (String ip_tmp : Peer.neighbours.keySet()) {
            if (Peer.neighbours.get(ip_tmp) == null) {
                String ip_to_create_connection[] = ip_tmp.split(":");
                Socket socket = new Socket(InetAddress.getByName(ip_to_create_connection[0]),
                        Integer.parseInt(ip_to_create_connection[1]));
                Peer.neighbours.replace(ip_tmp, null, socket);
            }
        }
    }

    public static void send_message(Message message_to_send)
            throws NumberFormatException, UnknownHostException, IOException {

        if (Peer.neighbours.containsValue(null)) {
            Server.build_network();
        }

        for (Socket socket : Peer.neighbours.values()) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message_to_send);
            out.flush();
            out.close();
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

            System.out.println("DEBUG");
            System.out.println(message_received);
            System.out.println("----");

            switch (message_received.getType_of_message()) {
                case "message":
                    Server.update_clocks(message_received);
                    Server.queue.add(message_received);
                    // Server.send_ack(message_received);
                    break;
                case "acknowledge":
                    Server.update_clocks(message_received);
                    if (!Server.queue.isEmpty()) {
                        Server.queue.add(message_received);
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

// TODO: Change this to use poisson;

// start_poisson_generator()
// lamport_clock = 0
// while(true) {
//       dt = next_poisson_interval()
//       sleep(dt)
//       send_message(lamport_clock++)
// }
class Client implements Runnable {
    private Scanner scanner;

    public Client() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            try {
                String input = scanner.nextLine();
                Message to_send = new Message(Peer.timestamp.getAndIncrement(), input);
                to_send.setType_of_message("message");
                Server.send_message(to_send);
            } catch (NumberFormatException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Message implements Comparable<Message> {
    private Long timestamp;
    private String message;
    private String type_of_message;

    public Message(Long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public Message parse(String readLine) {
        Scanner sc = new Scanner(readLine);
        String tmp = sc.next();
        this.type_of_message = tmp.replace(":", "");
        this.timestamp = Long.parseLong(sc.next());
        this.message = sc.nextLine();
        sc.close();
        return this;
    }

    public Message() {
    }

    @Override
    public int compareTo(Message arg0) {
        if (this.timestamp > arg0.timestamp) {
            return 1;
        } else if (this.timestamp < arg0.timestamp) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return type_of_message + ": " + timestamp + " " + message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType_of_message() {
        return type_of_message;
    }

    public void setType_of_message(String type_of_message) {
        this.type_of_message = type_of_message;
    }

}