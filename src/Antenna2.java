import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Antenna2 extends Antenna {
    private static final String ID = String.valueOf(2);

    public Antenna2() throws IOException, TimeoutException {
        super(ID);
    }

    public static void main(String[] args) {
        Antenna2 antenna;
        try {
            antenna = new Antenna2();
            antenna.start();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error initialising Antenna " + ID);
            e.printStackTrace();
        }
    }
}