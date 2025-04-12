import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Antenna1 extends Antenna {
    private static final String ID = String.valueOf(1);

    public Antenna1() throws IOException, TimeoutException {
            super(ID);
    }

    public static void main(String[] args) {
        Antenna1 antenna;
        try {
            antenna = new Antenna1();
            antenna.start();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error initialising Antenna " + ID);
            e.printStackTrace();
        }
    }
}
