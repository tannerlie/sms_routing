# Distributed Messaging System

A **RabbitMQ-based** messaging platform that simulates mobile users communicating through antennas and a central routing hub. Users move randomly in a grid while maintaining connectivity through the nearest antenna.

## Key Features
- 📡 **Dynamic antenna switching** as users move
- 📨 **Message routing** through central hub
- 🌐 **Distributed architecture** using RabbitMQ
- 🏃 **Simulated user mobility** in 20x20 grid

## Quick Start

### 1. Prerequisites
- Java 8+
- RabbitMQ server ([installation guide](https://www.rabbitmq.com/download.html))

### 2. Setup
```bash
# 1. Place these JARs in your src folder:
#    - amqp-client-5.16.0.jar  
#    - slf4j-api-1.7.36.jar  
#    - slf4j-simple-1.7.36.jar  

# 2. Compile all classes  
javac -cp amqp-client-5.16.0.jar *.java
```

### 3. Run System
Run each component in **separate terminals**:

**Central Hub** (message router):
```bash
java CentralHub
```

**Antennas** (4 infrastructure nodes):
```bash
java -cp .:amqp-client-5.16.0.jar:slf4j-*.jar Antenna1
java -cp .:amqp-client-5.16.0.jar:slf4j-*.jar Antenna2 
java -cp .:amqp-client-5.16.0.jar:slf4j-*.jar Antenna3
java -cp .:amqp-client-5.16.0.jar:slf4j-*.jar Antenna4
```

**Users** (launch multiple instances):
```bash
java -cp .:amqp-client-5.16.0.jar:slf4j-*.jar User
```

### 4. Start Messaging
1. Enter your username when prompted
2. Send messages in format:
   ```
   (to: username) your message here
   ```
3. Watch automatic routing as users move!

## System Architecture
```
[Users] → [Antennas] → [Central Hub] → [Antennas] → [Users]
       (Ping updates) ↘___________[Routing Table]↗
```

💡 **Tip**: Users automatically:
- Move every 5 seconds
- Send location pings every 1 second
- Switch antennas when closer to another node