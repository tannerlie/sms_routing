import com.rabbitmq.client.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.TimeoutException;

public class Antenna {

    private static final String TASK_QUEUE_NAME = "message_queue";
    private static final String CENTRAL_HUB_QUEUE_NAME = "central_hub_queue";
    private Connection connection;
    private Channel recvChannel;
    private Channel centralHubChannel;
    private RoutingTable routingTable;

    public Antenna(ConnectionFactory factory) throws IOException, TimeoutException {
        this.connection = factory.newConnection();
        this.recvChannel = connection.createChannel();
        this.centralHubChannel = connection.createChannel();
    }

    private void forwardMessageToCentral() {
        //
    }

    private void processPing(Message message, byte[] serializedMessage) {
        Route route = new Route(message.getFrom(), message.getBody());
        if (!routingTable.contains(route)) {
            routingTable.addRoute(route);
            routeToTarget(serializedMessage, centralHubChannel, CENTRAL_HUB_QUEUE_NAME);
        }
    }

    private void routeToTarget(byte[] message, Channel channel, String queue) {
        // check routing table
        try {
            channel.basicPublish("", queue, null, message);
        } catch (Exception e) {
            System.err.println("Failed to publish message to target: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("localhost");

        Antenna antenna = new Antenna(factory);

        // Add this before setting up the consumer
        antenna.recvChannel.queuePurge(TASK_QUEUE_NAME);
        antenna.recvChannel.queueDeclare(TASK_QUEUE_NAME, true,     false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        antenna.recvChannel.basicQos(1);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream in = new ObjectInputStream(bis);

            // Read and cast to your object type
            try {
                Message message = (Message) in.readObject();
                if (message.getTo().isEmpty()) {
//                    antenna.processPing(message, delivery.getBody());
//                    forwardMessageToCentral();
                } else {
                    String queue = "user_" + message.getTo() + "_queue";
                    System.out.println("received" + message.getBody() + " from " + message.getFrom());
                    antenna.routeToTarget(delivery.getBody(), antenna.recvChannel, queue);
                }
            } catch (Exception e) {
                System.out.println(" [x] Error deserializing message on server side: " + e.getMessage());
                e.printStackTrace();
            } finally {
                antenna.recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        antenna.recvChannel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }
}