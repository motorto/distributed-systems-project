package ds.assignment.multicast;

import java.util.Scanner;

public class Message implements Comparable<Message> {
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

    public Message(Long timestamp, String message, String type_of_message) {
        this.timestamp = timestamp;
        this.message = message;
        this.type_of_message = type_of_message;
        this.ip_addr_origin = Server.ip;
        this.ip_port_origin = Server.port;
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
