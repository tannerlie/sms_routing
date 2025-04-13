import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Antenna3 extends Antenna {
    private static final String ID = String.valueOf(3);

    public Antenna3() throws IOException, TimeoutException {
        super(ID);
    }

    public static void main(String[] args) {
        Antenna3 antenna;
        try {
            antenna = new Antenna3();
            antenna.start();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error initialising Antenna " + ID);
            e.printStackTrace();
        }
    }
}