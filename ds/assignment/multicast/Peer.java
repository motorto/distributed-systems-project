package ds.assignment.multicast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Peer
 */
public class Peer {

    static final AtomicLong myTimestamp = new AtomicLong();
    static final ArrayList<String> neighbors = new ArrayList<>();

    public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException {

        if (args.length < 3) {
            System.err.println("Missing Arguments: Peer my_ip my_port others_ip(ip:port)");
            System.exit(1);
        }

        for (int i = 2; i < args.length; i++) {
            Peer.neighbors.add(args[i]);
        }

        new Thread(new Server(args[0], args[1])).start();
        new Thread(new Client()).start();
    }
}

class Server implements Runnable {
    ServerSocket server;
    String ip_port, ip_addr;
    static final PriorityQueue<Message> queue = new PriorityQueue<>();
    static final HashMap<InetAddress, ArrayList<Message>> source = new HashMap<>();

    public Server(String ip_addr, String ip_port) throws NumberFormatException, UnknownHostException, IOException {
        this.ip_port = ip_port;
        this.ip_addr = ip_addr;
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
    }

    @Override
    public void run() {
        while (true) {
            try {

                Socket client = server.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                InetAddress sender = client.getInetAddress();

                Message message_received = new Message().parse(in.readLine());

                if (!source.containsKey(sender)) {
                    source.put(sender, new ArrayList<Message>());
                }
                ArrayList<Message> list_of_messages = source.get(sender);
                list_of_messages.add(message_received);

                long tc;
                for (;;) {
                    long oldValue = Peer.myTimestamp.get();
                    tc = Math.max(oldValue, message_received.getTimestamp()) + 1;
                    if (Peer.myTimestamp.compareAndSet(oldValue, tc))
                        break;
                }

                if (!queue.contains(message_received)) {
                    Server.send_message(new Message(tc, message_received.getMessage()));
                }

                Server.queue.add(message_received);

                while (!Server.queue.isEmpty()) {
                    Message tmp = Server.queue.poll();

                    if (!queue.contains(tmp)) {
                        System.out.println(tmp);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    static void send_message(Message message_to_send) {
        for (String client : Peer.neighbors) {
            try {
                String client_data[] = client.split(":");

                Socket socket = new Socket(InetAddress.getByName(client_data[0]), Integer.parseInt(client_data[1]));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println(message_to_send.toString());
                out.flush();

                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            String input = scanner.nextLine();
            Message to_send = new Message(Peer.myTimestamp.incrementAndGet(), input);

            Server.send_message(to_send);

        }
    }
}

class Message implements Comparable<Message> {
    private Long timestamp;
    private String message;

    public Message(Long timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public Message parse(String readLine) {
        Scanner sc = new Scanner(readLine);
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
        return timestamp + " " + message;
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
}