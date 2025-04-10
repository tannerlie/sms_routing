import java.io.Serializable;

public class Message implements Serializable {
    private String to;
    private String from;
    private String body;

    // Constructor
    public Message(String to, String from, String body) {
        this.to = to;
        this.from = from;
        this.body = body;
    }

    // Getters and Setters
    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    // Optional: Override toString() for easy printing
    @Override
    public String toString() {
        return "Message{" +
                "to='" + to + '\'' +
                ", from='" + from + '\'' +
                ", body='" + body + '\'' +
                '}';
    }
}
