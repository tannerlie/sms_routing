import com.rabbitmq.client.*;
import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class User {

    private static final String ANTENNA_1_QUEUE_NAME = "antenna_1_queue";
    private static final String ANTENNA_2_QUEUE_NAME = "antenna_2_queue";
    private static final int LOCATION_PING_INTERVAL = 1000; // 1 seconds
    private static final int LOCATION_MOVEMENT_INTERVAL = 5000; // 5 seconds
    private static final ArrayList<String> antenna_queues = new ArrayList<>(Arrays.asList("antenna_1_queue",
            "antenna_2_queue"));

    private final ConnectionFactory connectionFactory;
    private final Connection connection;
    private final Position position;
    private Thread locationPingThread;
    private Thread locationMovementThread;
    private Thread recvMessageThread;
    private String uid;

    public User(ConnectionFactory connectionFactory) throws IOException, TimeoutException {
        this.connectionFactory = connectionFactory;
        this.connection = connectionFactory.newConnection();
        this.position =  new Position();
    }

    private void close() throws IOException {
        connection.close();
    }

    private Runnable createLocationMovementTask(Position position) {
        return () -> {
            while (true) {
                try {
                    Thread.sleep(LOCATION_MOVEMENT_INTERVAL); // wait 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // preserve interrupt status
                    break;
                }
                position.move();

            }
        };
    }

    private void startLocationMovementThread() {
        Runnable movementTask = createLocationMovementTask(position);
        this.locationMovementThread = new Thread(movementTask);
        this.locationMovementThread.start();
    }

    private void stopLocationMovementTask() {
        this.locationMovementThread.interrupt();
    }

    private Runnable createLocationPingTask(Channel channel) {
        return () -> {
            while (true) {
                sendLocationPing(channel);  // send location ping to RabbitMQ
                try {
                    Thread.sleep(LOCATION_PING_INTERVAL); // wait 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // preserve interrupt status
                    break;
                }
            }
        };
    }

    private void startLocationPingTask(Channel channel) {
        try {

            channel.queueDeclare(ANTENNA_1_QUEUE_NAME, true, false, false, null);
            channel.queueDeclare(ANTENNA_2_QUEUE_NAME, true,false, false, null);

            // Start a thread for sending location pings every 5 seconds
            Runnable locationTask = createLocationPingTask(channel);
            this.locationPingThread = new Thread(locationTask);
            this.locationPingThread.start();

        } catch (Exception e) {
            System.err.println("Error in location ping task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopLocationPingTask() {
        this.locationPingThread.interrupt();
    }

    private void sendLocationPing(Channel channel) {
        // Placeholder for sending location to RabbitMQ
        Message message = new Message("", uid, position.toString());

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(message);
            out.flush();
            byte[] serializedObject = bos.toByteArray();
            publishMessage(channel, serializedObject);
        } catch (IOException e) {
            System.err.println("Error in serializing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(Channel channel, String input) throws IOException {
        try {

            channel.queueDeclare(ANTENNA_1_QUEUE_NAME, true, false, false, null);

            String targetUserId;
            String messageBody;

            // Find the closing parenthesis
            int closeParen = input.indexOf(')');
            if (closeParen != -1) {
                // Extract target user ID (between "to:" and ")")
                String targetPart = input.substring(0, closeParen);
                int toIndex = targetPart.indexOf("to:");
                if (toIndex != -1) {
                    targetUserId = targetPart.substring(toIndex + 3).trim();

                    // Extract message body (after the closing parenthesis)
                    messageBody = input.substring(closeParen + 1).trim();
                } else {
                    throw new IOException("Message is not in the correct format");
                }
            } else {
                throw new IOException("Message is not in the correct format");
            }

            Message message = new Message(targetUserId, uid, messageBody);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(message);
            out.flush();
            byte[] serializedObject = bos.toByteArray();

            // Create and send the message
            publishMessage(channel, serializedObject);

        } catch(IOException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void publishMessage(Channel channel, byte[] message) {
        try {
            channel.basicPublish("", ANTENNA_1_QUEUE_NAME, null, message);
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    private Runnable createRecvMessageThread(String receiveQueue, Channel recvChannel) {
        return () -> {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                ByteArrayInputStream bis = new ByteArrayInputStream(delivery.getBody());
                ObjectInputStream in = new ObjectInputStream(bis);

                try {
                    Message message = (Message) in.readObject();
                    System.out.println("Received message from: " + message.getFrom());
                    System.out.println(message.getBody());
                } catch (Exception e) {
                    System.err.println("Error in deserializing message on user side: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    recvChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            try {
                recvChannel.basicConsume(receiveQueue, false, deliverCallback, consumerTag -> {});
            } catch (IOException e) {
                System.err.println("Failed to recv message: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    public void startRecvMessageThread(Channel recvChannel) {
        try {
            recvChannel = connection.createChannel();
            String receiveQueue = "user_" + uid + "_queue";
            recvChannel.queueDeclare(receiveQueue, false, false, false, null);
            Runnable locationTask = createRecvMessageThread(receiveQueue, recvChannel);
            this.recvMessageThread = new Thread(locationTask);
            this.recvMessageThread.start();
        } catch (Exception e) {
            System.err.println("Failed to setup recv channel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopRecvChannelTask() {
        this.recvMessageThread.interrupt();
    }

    public static void main(String[] argv) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            User user = new User(factory);
            Channel locationPingChannel = user.connection.createChannel(); // Create ping channel
            Channel messagingChannel = user.connection.createChannel(); //Create messaging channel
            Channel recvChannel = user.connection.createChannel();

            Scanner scanner = new Scanner(System.in);
            String userInput;

            System.out.println("Please enter your username: ");

            String uid = scanner.nextLine();
            user.setUid(uid);

            user.startRecvMessageThread(recvChannel); // Start receiving message task
            user.startLocationPingTask(locationPingChannel); // Start location ping task
            user.startLocationMovementThread(); // Start location movement task

            System.out.println("Type in the format: (to: <username>) <message>");
            System.out.println("e.g. (to: user1) hello world");
            System.out.println("Enter \"exit\" to close the application");



            while (true) {
                userInput = scanner.nextLine();
                if (userInput.equals("exit")) {
                    user.stopRecvChannelTask();
                    user.stopLocationPingTask();
                    user.stopLocationMovementTask();
                    recvChannel.close();
                    locationPingChannel.close();
                    messagingChannel.close();
                    user.close(); // Close Connection
                    scanner.close(); // Release System.in
                    break;
                }
                try {
                    user.sendMessage(messagingChannel, userInput);
                } catch (IOException e) {
                    System.err.println("Failed to send message: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing User: " + e.getMessage());
            e.printStackTrace();
        }
    }
}