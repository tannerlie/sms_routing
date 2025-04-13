import java.io.Serializable;
import java.util.Random;

public class Position {
    private static final int MAX_DIMENSIONS = 20;
    private static final Random RANDOM = new Random();
    private static final int[][] DIRECTION = {{1,0}, {0,1}, {-1,0}, {0, -1}};

    private int x;
    private int y;

    public Position() {
        this.x = RANDOM.nextInt(MAX_DIMENSIONS);
        this.y = RANDOM.nextInt(MAX_DIMENSIONS);
    }

    public Position(int[] position) {
        this.x = position[0];
        this.y = position[1];
    }

    public void move() {
        int[] movement = DIRECTION[RANDOM.nextInt(4)];
        int xTemp = (x + movement[0]);
        x = xTemp >= 0 ? xTemp % MAX_DIMENSIONS : xTemp + MAX_DIMENSIONS;
        int yTemp = (y + movement[1]);
        y = yTemp >=0 ? yTemp % MAX_DIMENSIONS : yTemp + MAX_DIMENSIONS;
    }

    // Calculate distance to another position (Euclidean distance for simplicity)
    public double distanceTo(Position other) {
        return Math.sqrt((double) (Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2)));
    }

    @Override
    public String toString() {
        return x + "," + y;
    }
}
