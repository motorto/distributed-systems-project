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
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class Peer {

    static final AtomicLong timestamp = new AtomicLong();
    static boolean i_offer_services = false;
    static HashMap<String, Socket> network = new HashMap<>();
    static PriorityQueue<Message> queue = new PriorityQueue<>();

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
                        Peer.i_offer_services = true;
                        continue;
                    }
                    Peer.network.put(line_tmp, null);
                } else {
                    if (line.equals(my_ip)) {
                        continue;
                    }
                    Peer.network.put(line, null);
                }
            }
        }

        new Thread(new Server(args[0], args[1])).start();
        if (!i_offer_services) {
            new Thread(new Client()).start();
        }
    }
}

class Server implements Runnable {
    ServerSocket server;
    static String ip_port, ip_addr;

    public Server(String ip_addr, String ip_port) throws NumberFormatException, IOException {
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

    public static void update_clocks(Message message_received) {
        long old_value = Peer.timestamp.get();
        while (true) {
            if (Peer.timestamp.compareAndSet(old_value, Math.max(old_value, message_received.getTimestamp() + 1))) {
                break;
            }
        }
    }

    public static void send_message(boolean is_acknowledgement, Message to_send)
            throws NumberFormatException, UnknownHostException, IOException {

        if (!is_acknowledgement) {
            to_send.setType_of_message("message");
            to_send.setIp_addr_origin(Server.ip_addr);
            to_send.setIp_port_origin(Server.ip_port);

            for (String ip_tmp : Peer.network.keySet()) { // TODO: Why don't can we use the socket
                String ip_to_create_connection[] = ip_tmp.split(":");

                Socket socket = new Socket(InetAddress.getByName(ip_to_create_connection[0]),
                        Integer.parseInt(ip_to_create_connection[1]));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println(to_send);
                out.flush();
                out.close();
                socket.close();

            }
        } else {
            to_send.setType_of_message("ack");

            String ip_to_send = to_send.getIp_addr_origin();
            String port_to_send = to_send.getIp_port_origin();

            to_send.setIp_addr_origin(Server.ip_addr);
            to_send.setIp_port_origin(Server.ip_port);

            Socket socket = new Socket(InetAddress.getByName(ip_to_send),
                    Integer.parseInt(port_to_send));

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

            System.out.println("DEBUG");
            System.out.println(message_received);

            switch (message_received.getType_of_message()) {
                case "message":
                    Server.update_clocks(message_received);
                    Peer.queue.add(message_received);
                    Server.send_message(true, message_received);
                    break;
                case "ack":
                    Server.update_clocks(message_received);
                    Peer.queue.add(message_received);
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
                Server.send_message(false, to_send);
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
    private String ip_addr_origin, ip_port_origin;

    public String getIp_port_origin() {
        return ip_port_origin;
    }

    public void setIp_port_origin(String ip_port_origin) {
        this.ip_port_origin = ip_port_origin;
    }

    public String getIp_addr_origin() {
        return ip_addr_origin;
    }

    public void setIp_addr_origin(String ip_addr_origin) {
        this.ip_addr_origin = ip_addr_origin;
    }

    public Message(Long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public Message parse(String readLine) {
        Scanner sc = new Scanner(readLine);

        String tmp = sc.next(); // Reads the From:
        tmp = sc.next();

        String ip_origin[] = tmp.split(":");
        this.ip_addr_origin = ip_origin[0];
        this.ip_port_origin = ip_origin[1];

        tmp = sc.next(); // type of message
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
        return "From: " + ip_addr_origin + ":" + ip_port_origin + " " + type_of_message + ": " + timestamp + " "
                + message;
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
