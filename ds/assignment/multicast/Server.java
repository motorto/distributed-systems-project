package ds.assignment.multicast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;

/* O ficheiro é do formato:
 * 
 * ip:porta 
 * (1 por linha)
 * 
 * exemplo:
 * localhost:3000
 */
public class Server {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Missing Arguments: Peer my_ip my_port file_containing_all_other_ip_addr");
            System.exit(1);
        }

        new Thread(new Server_main(args[0], args[1], args[2])).start();
    }
}

class Server_main implements Runnable {
    ServerSocket server;
    String file_containing_ips;
    static String ip_addr;
    static String ip_port;
    static HashSet<String> other_server_locations;

    public Server_main(String ip_addr, String ip_port, String file_containing_ips)
            throws NumberFormatException, UnknownHostException, IOException {
        this.server = new ServerSocket(Integer.parseInt(ip_port), 1, InetAddress.getByName(ip_addr));
        Server_main.ip_addr = ip_addr;
        Server_main.ip_port = ip_port;
    }

    @Override
    public void run() {
        while (true) {
            try {
                process_file(file_containing_ips);
                Socket client = server.accept();
                new Thread(new Connection(client)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void process_file(String file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file_containing_ips))) {
            String line;
            while ((line = br.readLine()) != null) {
                other_server_locations.add(line);
            }
        }
    }

    /* 
    TODO: Está comentado pq não sei se quando o p5 recebe uma mensagem tenho que a mandar para o 
    p6 ( no enunciado fala de replicas se for o caso é se descomentar isto e a L103)
     * public static void gosssip(String message_received)
     * throws NumberFormatException, UnknownHostException, IOException {
     * for (String tmp : other_server_locations) {
     * String neighbour[] = tmp.split(":");
     * Socket socket = new Socket(InetAddress.getByName(neighbour[0]),
     * Integer.parseInt(neighbour[1]));
     * PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
     * out.println("sync " + message_received);
     * out.flush();
     * out.close();
     * }
     * }
     */
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
            String message_received = parser.nextLine();

            System.out.println(message_received);
            /*
             * Server_main.gosssip(message_received);
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}