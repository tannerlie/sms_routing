import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CentralHub {
    private static final String CENTRAL_HUB_ROUTE_QUEUE_NAME = "central_hub_route_queue";
    private static final String CENTRAL_HUB_MESSAGE_QUEUE_NAME = "central_hub_message_queue";
    private static final String ANTENNA_1 = "antenna_1";
    private static final String ANTENNA_2 = "antenna_2";
    private static final String ANTENNA_3 = "antenna_3";
    private static final String ANTENNA_4 = "antenna_4";
    private Map<String, String> antennaQueueMapping;
    private Connection connection;
    private Channel recvChannel;
    private Channel sendChannel;
    private RoutingTable routingTable;

    public CentralHub(ConnectionFactory connectionFactory) throws IOException, TimeoutException {
        this.connection = connectionFactory.newConnection();
        this.recvChannel = connection.createChannel();
        this.sendChannel = connection.createChannel();
        this.antennaQueueMapping = new HashMap<>();
        for (int i = 1; i < 5; i += 1) {
            antennaQueueMapping.put("antenna_" + i, "antenna_" + i + "_message_queue");
        }
        this.routingTable = new RoutingTable();
    }

    private void updateRoutingTable(Route route) {
        if (routingTable.contains(route)) {
            routingTable.updateAntenna(route);
        } else {
            routingTable.addRoute(route);
        }
    }

    void routeMessage(String antenna, byte[] serializedMessage) {
        String queue = antennaQueueMapping.get(antenna);
        try {
            sendChannel.basicPublish("", queue, null, serializedMessage);
            System.out.println("Routing Message to " + queue);
        } catch(IOException e) {
            System.err.println("Error routing message to" + antenna + " on client side: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] argv) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        CentralHub centralHub = new CentralHub(factory);
        centralHub.recvChannel.queueDeclare(CENTRAL_HUB_MESSAGE_QUEUE_NAME, true,     false, false, null);
        centralHub.recvChannel.queueDeclare(CENTRAL_HUB_ROUTE_QUEUE_NAME, true,     false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        centralHub.recvChannel.basicQos(1);

        DeliverCallback deliverRouteCallback = (consumerTag, delivery) -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream in = new ObjectInputStream(bis);
            try {
                Route route = (Route) in.readObject();
                centralHub.updateRoutingTable(route);
            } catch (Exception e) {
                System.err.println("Error in deserializing message on central hub side: " + e.getMessage());
                e.printStackTrace();
            } finally {
                centralHub.recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        DeliverCallback deliverMessageCallback = (consumerTag, delivery) -> {
            ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream in = new ObjectInputStream(bis);
            try {
                Message message = (Message) in.readObject();
                String antennaToRouteTo = centralHub.routingTable.findAntenna(message.getTo());
                System.out.println("Received " + message.getBody() + " from " + message.getFrom());
                centralHub.routeMessage(antennaToRouteTo, delivery.getBody());
            } catch (ClassNotFoundException e) {
                System.err.println("Error in deserializing message on central hub side: " + e.getMessage());
                e.printStackTrace();
            } finally {
                centralHub.recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        centralHub.recvChannel.basicConsume(CENTRAL_HUB_MESSAGE_QUEUE_NAME, false, deliverMessageCallback,
                consumerTag -> {});
        centralHub.recvChannel.basicConsume(CENTRAL_HUB_ROUTE_QUEUE_NAME, false, deliverRouteCallback,
                consumerTag -> {});
    }
}
