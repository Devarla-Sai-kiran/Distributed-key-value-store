package core;

import java.util.*;

public class RingManager {
    private final TreeMap<String, String> ring = new TreeMap<>();
    private final Map<String, Node> nodeMap = new HashMap<>();

    public void addNode(Node node) {
        String nodeId = node.getId();
        String hash = HashUtil.sha1(nodeId);
        ring.put(hash, nodeId);
        nodeMap.put(nodeId, node);
        System.out.println("Node added: " + nodeId + " (Hash: " + hash + ")");
    }

    public void removeNode(String nodeId) {
        String hash = HashUtil.sha1(nodeId);
        ring.remove(hash);
        nodeMap.remove(nodeId);
        System.out.println("Node removed: " + nodeId);
    }

    public String getNodeForKey(String key) {
        String hash = HashUtil.sha1(key);
        SortedMap<String, String> tailMap = ring.tailMap(hash);
        String nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);
    }

    public List<String> getNNodesForKey(String key, int n) {
        List<String> nodes = new ArrayList<>();
        if (ring.isEmpty() || n <= 0) return nodes;

        String hash = HashUtil.sha1(key);
        SortedMap<String, String> tailMap = ring.tailMap(hash);

        Iterator<String> iterator = tailMap.isEmpty() ? ring.keySet().iterator() : tailMap.keySet().iterator();

        Set<String> visited = new HashSet<>();
        while (nodes.size() < n && iterator.hasNext()) {
            String nodeHash = iterator.next();
            String nodeId = ring.get(nodeHash);
            if (!visited.contains(nodeId)) {
                nodes.add(nodeId);
                visited.add(nodeId);
            }
        }

        if (nodes.size() < n) {
            iterator = ring.keySet().iterator();
            while (nodes.size() < n && iterator.hasNext()) {
                String nodeHash = iterator.next();
                String nodeId = ring.get(nodeHash);
                if (!visited.contains(nodeId)) {
                    nodes.add(nodeId);
                    visited.add(nodeId);
                }
            }
        }

        return nodes;
    }


    public String getNodeAddress(String nodeId) {
        Node node = nodeMap.get(nodeId);
        if (node != null) {
            return node.getIp() + ":" + node.getPort();
        }
        return null;
    }

    public void printRing() {
        for (Map.Entry<String, String> entry : ring.entrySet()) {
            System.out.println(entry.getValue() + " -> " + entry.getKey());
        }
    }

    public int getRingSize() {
        return ring.size();
    }
}
