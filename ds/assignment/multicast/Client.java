package ds.assignment.multicast;

import java.io.IOException;
import java.util.Scanner;

public class Client implements Runnable {
    private Scanner scanner;

    public Client() {
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

                Message to_send = new Message(Peer.timestamp.getAndIncrement(), input);
                Peer.queue.add(to_send);
                Server.send_message(0, to_send);
            } catch (NumberFormatException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
