package ui;

import node.NodeClient;
import node.Message;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DynamoUI extends JFrame {

    private JTextField keyField;
    private JTextField valueField;
    private JTextArea logArea;
    private RingPanel ringPanel;
    private NodeClient client;
    private JComboBox<String> nodeSelector;

    private final Map<String, Integer> nodePortMap = new HashMap<>();

    public DynamoUI() {
        setTitle("DynamoDB Mini - UI");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        client = new NodeClient(); // initialize backend client

        // Define node-port mapping
        nodePortMap.put("Node-A", 5000);
        nodePortMap.put("Node-B", 5001);
        nodePortMap.put("Node-C", 5002);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // --- Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.add(new JLabel("Key:"));
        keyField = new JTextField(10);
        inputRow.add(keyField);

        inputRow.add(new JLabel("Val:"));
        valueField = new JTextField(10);
        inputRow.add(valueField);

        inputRow.add(new JLabel("Node:"));
        nodeSelector = new JComboBox<>(new String[]{"Node-A", "Node-B", "Node-C"});
        inputRow.add(nodeSelector);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton putButton = new JButton("PUT");
        JButton getButton = new JButton("GET");
        JButton markDownButton = new JButton("Mark DOWN");
        JButton markUpButton = new JButton("Mark UP");

        buttonRow.add(putButton);
        buttonRow.add(getButton);
        buttonRow.add(markDownButton);
        buttonRow.add(markUpButton);

        topPanel.add(inputRow);
        topPanel.add(buttonRow);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- Center: Ring Panel + Logs
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        ringPanel = new RingPanel();
        ringPanel.setPreferredSize(new Dimension(600, 220));
        ringPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(ringPanel);

        ArrayList<String> mockNodes = new ArrayList<>();
        mockNodes.add("Node-A");
        mockNodes.add("Node-B");
        mockNodes.add("Node-C");
        ringPanel.setNodes(mockNodes);

        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(scrollPane);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        // --- Button Actions
        putButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            String val = valueField.getText().trim();
            String selectedNode = (String) nodeSelector.getSelectedItem();
            int port = nodePortMap.get(selectedNode);

            log("PUT clicked to " + selectedNode + " with Key=" + key + ", Value=" + val);
            try {
                String response = client.sendPutRequest(key, val, "localhost", port);
                log("PUT Response: " + response);
            } catch (Exception ex) {
                log("PUT Error: " + ex.getMessage());
            }
        });

        getButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            String selectedNode = (String) nodeSelector.getSelectedItem();
            int port = nodePortMap.get(selectedNode);

            log("GET clicked from " + selectedNode + " with Key=" + key);
            try {
                String value = client.sendGetRequest(key, "localhost", port);
                log("GET Response: " + value);
            } catch (Exception ex) {
                log("GET Error: " + ex.getMessage());
            }
        });

        markDownButton.addActionListener(e -> {
            String selectedNode = (String) nodeSelector.getSelectedItem();
            ringPanel.setNodeStatus(selectedNode, NodeStatus.FAILED);
            log(selectedNode + " marked as DOWN (FAILED)");
        });

        markUpButton.addActionListener(e -> {
            String selectedNode = (String) nodeSelector.getSelectedItem();
            ringPanel.setNodeStatus(selectedNode, NodeStatus.ACTIVE);
            log(selectedNode + " marked as UP (ACTIVE)");

            // Trigger recovery
            int port = nodePortMap.get(selectedNode);
            try {
                Message recovery = new Message(Message.MessageType.RECOVERY, null, null, selectedNode);
                Message response = NodeClient.sendMessage("localhost", port, recovery);
                log("Recovery triggered for " + selectedNode + " → Keys: "
                        + (response != null && response.getValue() != null ? response.getValue() : "none"));
            } catch (Exception ex) {
                log("Recovery Error: " + ex.getMessage());
            }
        });
    }

    public void log(String message) {
        logArea.append("[LOG] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DynamoUI::new);
    }
}
