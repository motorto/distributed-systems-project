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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import ds.assignment.multicast.poisson.PoissonProcess;

public class Peer {
    static final AtomicLong timestamp = new AtomicLong();
    static HashSet<String> network = new HashSet<>();
    static PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>();
    static boolean i_offer_service = false;

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
                        Peer.i_offer_service = true;
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

        if (Peer.i_offer_service)
            new Thread(new Mock_Server(Peer.network.size() + 1)).start();

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Press the Enter key when network is properly created");
            scanner.nextLine();
        }

        if (!Peer.i_offer_service) {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Press the Enter key when network is properly created");
                scanner.nextLine();
            }
            new Thread(new Generate_Messages()).start();
        }
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
                System.exit(-1);
            }
        }
    }

    public static void update_clocks(Message message_received) {
        long old_value = Peer.timestamp.get();
        while (true) {
            if (Peer.timestamp.compareAndSet(old_value, Math.max(old_value, message_received.getTimestamp()) + 1)) {
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

            switch (message_received.getType_of_message()) {
                case "message":
                    Message to_ack = new Message(Peer.timestamp.getAndIncrement(), message_received.getMessage(),
                            "ack");
                    Server.send_message(to_ack);
                    break;
                case "ack":
                    break;
                default:
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

class Generate_Messages implements Runnable {
    String file_path = "ds/assignment/multicast/dictionary.txt";
    Random random = new Random();

    public Generate_Messages() {
    }

    @Override
    public void run() {
        Random rng = new Random(0); // base RNG to use
        double lambda = 60; // rate parameter
        PoissonProcess pp = new PoissonProcess(lambda, rng);
        while (true) {
            try {
                double poisson = pp.timeForNextEvent() * 60;
                int random_line = random.nextInt(count_lines_of_file(file_path));

                int round_number = (int) Math.round(poisson);

                Thread.sleep(round_number * 1000);

                String word_to_send = Files.readAllLines(Paths.get(file_path)).get(random_line);

                Message to_send = new Message(Peer.timestamp.getAndIncrement(), word_to_send, "message");
                Server.send_message(to_send);
            } catch (NumberFormatException | IOException | InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private int count_lines_of_file(String file_path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file_path));
        int lines = 0;
        while (reader.readLine() != null)
            lines++;
        reader.close();
        return lines;
    }
}

class Mock_Server implements Runnable {

    int number_of_peers;

    Mock_Server(int number_of_peers) {
        this.number_of_peers = number_of_peers;
    }

    private void clean_queue(ArrayList<Message> positions_to_delete) {
        for (Message message_to_remove : positions_to_delete) {
            Peer.queue.remove(message_to_remove);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!Peer.queue.isEmpty()) {
                ArrayList<Message> messages_to_delete = new ArrayList<>();
                Message at_the_top = Peer.queue.peek();

                for (Message tmp : Peer.queue) {
                    if (at_the_top.getMessage().equals(tmp.getMessage())) {

                        if (tmp.getType_of_message().equals("message"))
                            at_the_top = tmp;
                        messages_to_delete.add(tmp);
                    }
                    if (messages_to_delete.size() == this.number_of_peers) {
                        clean_queue(messages_to_delete);
                        System.out.println(at_the_top);
                        break;
                    }
                }
            }
        }
    }

}