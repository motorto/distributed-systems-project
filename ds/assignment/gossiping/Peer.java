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
import java.lang.Math;
import ds.assignment.gossiping.poisson.PoissonProcess;

/**
 * Peer
 */
public class Peer {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Missing Arguments: Peer my_ip my_port file_to_propagate");
            System.exit(1);
        }

        new Thread(new Server(args[0], args[1])).start();
        new Thread(new Client()).start();
        new Thread(new Word_generator(args[2])).start();
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
        for (String string : neighbours.keySet()) {
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
    Random random = new Random();

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
                    }else {
                        float keep_gossiping = random.nextFloat(); 
                        if (keep_gossiping > 1/5) {
                            Server.gossip(word_received);
                        }
                        else{
                            System.out.println("Stop propagating: " + word_received);
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
    Scanner scanner;
    static Thread th;

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
        Random rng = new Random(0); // base RNG to use
        double lambda = 2; // rate parameter
        PoissonProcess pp = new PoissonProcess(lambda, rng);
        Random random = new Random();

        while (true) {
            try {
                double poisson = pp.timeForNextEvent() * 60;
                int random_line = random.nextInt(count_lines_of_file(file_path));
                
                int round_number = (int) Math.round(poisson);
                
                Thread.sleep(round_number * 1000);

                String word_generated = Files.readAllLines(Paths.get(file_path)).get(random_line);

                if (!Server.dictionary.contains(word_generated)) {
                    Server.dictionary.add(word_generated);
                    System.out.println("Word Generated: " + word_generated);
                    Server.gossip(word_generated);
                }
                else {
                    float keep_gossiping = random.nextFloat(); 
                    if (keep_gossiping > 1/5) {
                        Server.gossip(word_generated);
                    }
                    else{
                        System.out.println("Stop propagating: " + word_generated);
                    }
                }
                
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
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

    public static int rand_int_generator(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
