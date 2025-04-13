import java.util.ArrayList;
import java.util.Arrays;

public class GeographicalGrid {
    private static final int GRID_LENGTH = 20;
    private static final int ANTENNA_RADIUS = 8;
    private static final int[][] ANTENNA_COORDINATES = {
            {5, 5},
            {5, 15},
            {15, 5},
            {15, 15}
    };
    private static ArrayList<Position> antennaPositions;

    public GeographicalGrid() {
        this.antennaPositions = new ArrayList<>();
        for (int i = 0; i < ANTENNA_COORDINATES.length; i++) {
            Position position = new Position(ANTENNA_COORDINATES[i]);
            antennaPositions.add(position);
        }
    }

    public int connectToAntenna(Position position) {
        double minDist = GRID_LENGTH;
        int antenna = 0;
        int count = 0;
        for (Position antennaPosition : antennaPositions) {
            count += 1;
            double dist = position.distanceTo(antennaPosition);
            if (dist < minDist) {
                minDist = dist;
                antenna = count;
            }
        }
        return antenna;
    }

}
