package node;

import core.RingManager;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeServer {

    private int port;
    private String nodeId;
    private RingManager ring;
    private int replicationFactor;

    private final Map<String, String> dataStore = new ConcurrentHashMap<>();
    private final ExecutorService replicationExecutor = Executors.newFixedThreadPool(3);
    private volatile boolean running = true;

    private ServerSocket serverSocket;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public NodeServer(int port, String nodeId, RingManager ring, int replicationFactor) {
        this.port = port;
        this.nodeId = nodeId;
        this.ring = ring;
        this.replicationFactor = replicationFactor;

    }

    private void fetchMissingDataFromReplicas() {
        List<String> replicas = ring.getNNodesForKey("recovery:" + nodeId, replicationFactor);
        for (String replicaId : replicas) {
            if (replicaId.equals(nodeId)) continue;

            try {
                String host = "localhost";
                int port = getPortFromNodeId(replicaId); // ✅ FIXED

                Message recoveryRequest = new Message(Message.MessageType.RECOVERY, null, null, nodeId);
                Message response = NodeClient.sendMessage(host, port, recoveryRequest);

                if (response != null && response.getType() == Message.MessageType.RESPONSE && response.getValue() != null) {
                    String serialized = response.getValue(); // key1=value1;key2=value2
                    String[] pairs = serialized.split(";");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) {
                            dataStore.put(kv[0], kv[1]);
                        }
                    }
                    System.out.println("Recovered from " + replicaId + ": " + dataStore.keySet());
                }
            } catch (Exception e) {
                System.err.println("Recovery failed from " + replicaId + ": " + e.getMessage());
            }
        }
    }


    public void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                serverSocket = server;
                log(nodeId + " listening on port " + port);

                // Delay recovery slightly to allow all nodes to start
                new Thread(() -> {
                    try {
                        Thread.sleep(2000); // Wait 2 seconds for other servers to come up
                        fetchMissingDataFromReplicas();  // Now safe to call
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                while (running) {
                    Socket clientSocket = server.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                } else {
                    log(nodeId + " Server stopped.");
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        replicationExecutor.shutdown();
        log(nodeId + " server and executor stopped.");
    }

    private class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ) {
                Message message = (Message) in.readObject();
                log("Received: " + message);

                Message response;

                switch (message.getType()) {
                    case RECOVERY:
                        StringBuilder builder = new StringBuilder();
                        for (Map.Entry<String, String> entry : dataStore.entrySet()) {
                            builder.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                        }
                        String recovered = builder.toString();
                        response = new Message(Message.MessageType.RESPONSE, null, recovered, nodeId);
                        break;

                    case PUT:
                        dataStore.put(message.getKey(), message.getValue());
                        log("Stored: " + message.getKey() + " -> " + message.getValue());

                        List<String> responsibleNodes = ring.getNNodesForKey(message.getKey(), replicationFactor);
                        if (responsibleNodes.get(0).equals(nodeId)) {
                            forwardToReplicas(message);
                        }


                        response = new Message(Message.MessageType.RESPONSE, message.getKey(), message.getValue(), nodeId);
                        break;

                    case GET:
                        String value = dataStore.getOrDefault(message.getKey(), "NOT_FOUND");
                        log("GET: " + message.getKey() + " => " + value);
                        response = new Message(Message.MessageType.RESPONSE, message.getKey(), value, nodeId);
                        break;

                    default:
                        response = new Message(Message.MessageType.RESPONSE, "ERROR", "Unsupported operation", nodeId);
                }

                out.writeObject(response);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void forwardToReplicas(Message message) {
        System.out.println("Current Ring:");
        ring.printRing();

        List<String> replicas = ring.getNNodesForKey(message.getKey(), replicationFactor);
        System.out.println("Replicas for key " + message.getKey() + ": " + replicas);

        for (String replicaNode : replicas) {
            if (replicaNode.equals(this.nodeId)) continue;

            replicationExecutor.submit(() -> {
                try {
                    int replicaPort = getPortFromNodeId(replicaNode);
                    log("Forwarding PUT to replica: " + replicaNode);

                    Message replicaPut = new Message(Message.MessageType.PUT, message.getKey(), message.getValue(), nodeId);
                    Message resp = NodeClient.sendMessage("localhost", replicaPort, replicaPut);

                    log("Replica " + replicaNode + " response: " + resp);
                } catch (Exception e) {
                    System.err.println("Failed to forward PUT to replica " + replicaNode);
                    e.printStackTrace();
                }
            });
        }
    }
    
    public void shutdownGracefully() {
        running = false;
        try {
            serverSocket.close();
            System.out.println("[" + nodeId + "] Gracefully shut down.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private int getPortFromNodeId(String nodeId) {
        return switch (nodeId) {
            case "Node-A" -> 5000;
            case "Node-B" -> 5001;
            case "Node-C" -> 5002;
            default -> throw new IllegalArgumentException("Unknown nodeId: " + nodeId);
        };
    }

    public void putData(String key, String value) {
        dataStore.put(key, value);
        log("Replica PUT: " + key + " -> " + value);
    }

    public String getData(String key) {
        return dataStore.getOrDefault(key, "NOT_FOUND");
    }

    private void log(String message) {
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "][" + nodeId + "] " + message);
    }
}
