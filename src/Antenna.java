import com.rabbitmq.client.*;

import java.io.*;
import java.util.concurrent.TimeoutException;

public class Antenna {

    private static final String CENTRAL_HUB_ROUTE_QUEUE_NAME = "central_hub_route_queue";
    private static final String CENTRAL_HUB_MESSAGE_QUEUE_NAME = "central_hub_message_queue";
    private final String recvFromCentralQueueName;
    private final String antennaId;
    private final ConnectionFactory factory;
    private final String recvQueueName;
    private final Channel recvChannel;
    private final Channel sendChannel;

    public Antenna(String antennaId) throws IOException, TimeoutException {
        this.factory = new ConnectionFactory();
        this.factory.setHost("localhost");
        Connection connection = factory.newConnection();
        this.recvChannel = connection.createChannel();
        this.sendChannel = connection.createChannel();
        this.recvQueueName = "antenna_" + antennaId + "_queue";
        this.antennaId = "antenna_" + antennaId;
        this.recvFromCentralQueueName = "antenna_" + antennaId + "_message_queue";
//        this.routingTable = new RoutingTable();
    }

    private void processPing(Message message) {
        Route route = new Route(message.getFrom(), antennaId);
//        if (!routingTable.contains(route)) {
//            routingTable.addRoute(route);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(route);
            out.flush();
            byte[] serializedObject = bos.toByteArray();
            sendMessage(sendChannel, serializedObject, CENTRAL_HUB_ROUTE_QUEUE_NAME);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
//        }
    }

    private void sendMessage(Channel channel, byte[] message, String queue) throws IOException {
        try {
            channel.basicPublish("", queue, null, message);
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    private void routeToCentral(byte[] serializedMessage, Message message) {
        // check routing table
        Route route = new Route(message.getTo(), antennaId);
//        if (routingTable.contains(route)) {
//            String queue = "user_" + message.getTo() + "_queue";
//            try {
//                sendMessage(sendChannel, serializedMessage, queue);
//            } catch (Exception e) {
//                System.err.println("Failed to publish message to client: " + e.getMessage());
//                e.printStackTrace();
//            }
//        } else {
            try {
                sendMessage(sendChannel, serializedMessage, CENTRAL_HUB_MESSAGE_QUEUE_NAME);
            } catch (Exception e) {
                System.err.println("Failed to publish message to central hub: " + e.getMessage());
                e.printStackTrace();
            }
//        }
    }

    public void start() throws IOException {
        factory.setHost("localhost");

        recvChannel.queuePurge(recvQueueName);
        recvChannel.queueDeclare(recvQueueName, true,     false, false, null);
        recvChannel.queuePurge(recvFromCentralQueueName);
        recvChannel.queueDeclare(recvFromCentralQueueName, true, false, false, null);
//        antenna.recvCentralChannel.queueDeclare(CENTRAL_HUB_MESSAGE_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        recvChannel.basicQos(1);

        DeliverCallback processMessageFromUser = (consumerTag, delivery) -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream in = new ObjectInputStream(bis);

            // Read and cast to your object type
            try {
                Message message = (Message) in.readObject();
                if (message.getTo().isEmpty()) {
                    processPing(message);
                } else {
                    System.out.println("received" + message.getBody() + " from " + message.getFrom());
                    routeToCentral(delivery.getBody(), message);
                }
            } catch (Exception e) {
                System.out.println(" [x] Error deserializing message on antenna side: " + e.getMessage());
                e.printStackTrace();
            } finally {
                recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        DeliverCallback processMessageFromCentralHub = (consumerTag, delivery) -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream in = new ObjectInputStream(bis);

            try {
                Message message = (Message) in.readObject();
                String queue = "user_" + message.getTo() + "_queue";
                sendMessage(sendChannel, delivery.getBody(), queue);
                System.out.println("Sending " + message.getBody() + " to " + message.getTo());
            } catch (IOException e) {
                System.err.println("Failed to publish message to user: " + e.getMessage());
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Error deserializing message from central on antenna side: " + e.getMessage());
                e.printStackTrace();
            } finally {
                recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        recvChannel.basicConsume(recvQueueName, false, processMessageFromUser, consumerTag -> { });
        recvChannel.basicConsume(recvFromCentralQueueName, false, processMessageFromCentralHub,
                consumerTag -> { });
    }
}