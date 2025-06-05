package node;

import java.io.*;
import java.net.*;

public class NodeClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8000;

    public static Message sendMessage(String host, int port, Message message) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // 2s timeout
            socket.setSoTimeout(3000); // 3s read timeout

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(message);
            out.flush();

            return (Message) in.readObject();

        } catch (Exception e) {
            System.err.println("Failed to connect to " + host + ":" + port);
            e.printStackTrace();
            return null;
        }
    }

    public static String sendPutRequest(String key, String value, String host, int port) {
        Message put = new Message(Message.MessageType.PUT, key, value, "UI");
        Message resp = sendMessage(host, port, put);
        return resp != null ? resp.toString() : "PUT failed or no response for key: " + key;
    }

    public static String sendGetRequest(String key, String host, int port) {
        Message get = new Message(Message.MessageType.GET, key, null, "UI");
        Message resp = sendMessage(host, port, get);
        return resp != null ? resp.toString() : "GET failed or no response for key: " + key;
    }

}
