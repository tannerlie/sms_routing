import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Antenna4 extends Antenna {
    private static final String ID = String.valueOf(4);

    public Antenna4() throws IOException, TimeoutException {
        super(ID);
    }

    public static void main(String[] args) {
        Antenna4 antenna;
        try {
            antenna = new Antenna4();
            antenna.start();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error initialising Antenna " + ID);
            e.printStackTrace();
        }
    }
}
