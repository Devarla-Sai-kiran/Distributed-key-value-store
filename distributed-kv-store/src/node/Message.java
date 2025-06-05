package node;

import java.io.Serializable;

public class Message implements Serializable {

    public enum MessageType {
        RECOVERY,
        SHUTDOWN,
        PUT,
        GET,
        RESPONSE
    }

    private final MessageType type;
    private final String key;
    private final String value;
    private final String source;

    public Message(MessageType type, String key, String value, String source) {
        this.type = type;
        this.key = (key != null) ? key : "";
        this.value = (value != null) ? value : "";
        this.source = (source != null) ? source : "";
    }

    public MessageType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[" + type + "] From: " + source + ", Key: " + key + ", Value: " + value;
    }
}
