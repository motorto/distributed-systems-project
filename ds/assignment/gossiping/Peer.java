package ds.assignment.gossiping;

import java.io.BufferedReader;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Peer
 */
public class Peer {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Missing Arguments: Peer my_ip my_port data_to_disseminate");
            System.exit(1);
        }

        new Thread(new Server(args[0], args[1])).start();
        new Thread(new Client(args[2])).start();
    }
}

class Server implements Runnable {
    ServerSocket server;
    static HashMap<String, Socket> neighbours;
    static HashSet<String> dictionary;
    static String ip_addr;
    static String ip_port;

    public Server(String ip_addr, String ip_port) throws NumberFormatException, UnknownHostException, IOException {
        Server.ip_addr = ip_addr;
        Server.ip_port = ip_port;
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
        Server.neighbours = new HashMap<>();
        Server.dictionary = new HashSet<>();
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

        if (!neighbours.containsKey(new_node)) {
            System.out.println("Added: " + new_node);
            Socket socket = new Socket(InetAddress.getByName(neigh_ip_addr), Integer.parseInt(neigh_ip_port));
            neighbours.put(new_node, socket);

            if (!is_acknowledgement) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("register " + "acknowledgement " + ip_addr + " " + ip_port);
                out.flush();
                out.close();
            }
        }
    }

    public static void gossip(String word_to_send) throws IOException {
        for (String string : neighbours.keySet()) { // TODO: shouldn't I be able to use the already stored socket ??? It
                                                    // seems that out.close on L74 closes the socket as well.
            String ip_gossip[] = string.split(":");

            Socket socket = new Socket(InetAddress.getByName(ip_gossip[0]), Integer.parseInt(ip_gossip[1]));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("word " + word_to_send);
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
            switch (parser.next()) {
                case "register":
                    String is_acknowledgement = parser.next();
                    if (is_acknowledgement.equals("acknowledgement")) {
                        Server.register(parser.next(), parser.next(), true);
                    } else {
                        Server.register(is_acknowledgement, parser.next(), false);
                    }
                    break;
                case "word":
                    String word_received = parser.next();
                    if (!Server.dictionary.contains(word_received)) {
                        Server.dictionary.add(word_received);
                        System.out.println("Word Received: " + word_received);
                        Server.gossip(word_received);
                    } else {
                        int keep_gossiping = rand_int_generator(1, 5);
                        if (keep_gossiping == 5) {
                            Server.gossip(word_received);
                        }
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

    public static int rand_int_generator(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}

/**
 * Command Line
 */
class Client implements Runnable {
    String file_path;
    Scanner scanner;

    public Client(String file) throws Exception {
        this.scanner = new Scanner(System.in);
        this.file_path = file;
        new Thread(new Word_generator(this.file_path)).start();
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
                    System.err.println("Invalid Call");
                    break;
            }
        }
    }
}

class Word_generator implements Runnable {

    int file_number_lines;
    String file_path;


    public Word_generator(String file_path) throws IOException {
        this.file_path = file_path;
        this.file_number_lines = count_lines_of_file(this.file_path);
    }

    @Override
    public void run() {
        while (true) {
            try {
                int random_poisson_number = get_poisson_random();
                while (random_poisson_number > this.file_number_lines) {
                    random_poisson_number = get_poisson_random();
                }
                String word_generated = Files.readAllLines(Paths.get(file_path)).get(random_poisson_number);

                if (!Server.dictionary.contains(word_generated)) {
                    Server.dictionary.add(word_generated);
                    System.out.println("Word Generated: " + word_generated);
                    Server.gossip(word_generated);
                } else {
                    int keep_gossiping = rand_int_generator(1, 5);
                    if (keep_gossiping == 5) {
                        Server.gossip(word_generated);
                    }
                }
                Thread.sleep(30 * 1000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // From:
    // https://stackoverflow.com/questions/1277880/how-can-i-get-the-count-of-line-in-a-file-in-an-efficient-way
    private int count_lines_of_file(String file_path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file_path));
        int lines = 0;
        while (reader.readLine() != null)
            lines++;
        reader.close();
        return lines;
    }


    // From:
    // https://en.wikipedia.org/wiki/Poisson_distribution#Random_variate_generation
    private static int get_poisson_random() {
        double lambda = 2;
        Random r = new Random();
        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            p = p * r.nextDouble();
            k++;
        } while (p > L);
        return k - 1;
    }

    public static int rand_int_generator(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}
