package ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class RingPanel extends JPanel {
    private List<String> nodes = new ArrayList<>();
    private Map<String, NodeStatus> nodeStatusMap = new HashMap<>();

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
        // Set default status to ACTIVE
        for (String node : nodes) {
            nodeStatusMap.putIfAbsent(node, NodeStatus.ACTIVE);
        }
        repaint();
    }

    public void setNodeStatus(String nodeId, NodeStatus status) {
        nodeStatusMap.put(nodeId, status);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 3;
        int centerX = width / 2;
        int centerY = height / 2;

        int numNodes = nodes.size();
        for (int i = 0; i < numNodes; i++) {
            double angle = 2 * Math.PI * i / numNodes;
            int x = centerX + (int)(radius * Math.cos(angle)) - 25;
            int y = centerY + (int)(radius * Math.sin(angle)) - 25;

            String nodeId = nodes.get(i);
            NodeStatus status = nodeStatusMap.getOrDefault(nodeId, NodeStatus.ACTIVE);

            // Set color based on node status
            switch (status) {
                case ACTIVE:
                    g.setColor(Color.GREEN);
                    break;
                case REPLICA:
                    g.setColor(Color.CYAN); // Changed from yellow to cyan for contrast
                    break;
                case FAILED:
                    g.setColor(Color.RED);
                    break;
            }

            g.fillOval(x, y, 50, 50);
            g.setColor(Color.BLACK);
            g.drawString(nodeId, x + 8, y + 30);
        }
    }
}
