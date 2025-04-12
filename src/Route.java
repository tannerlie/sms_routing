import java.io.Serializable;

public class Route implements Serializable {
    private String targetUid;
    private String antennaId;

    public Route(String targetUid, String antenna) {
        this.targetUid = targetUid;
        this.antennaId = antenna;
    }
    public String getTargetUid() {
        return targetUid;
    }

    public String getAntennaId() {
        return antennaId;
    }

    public void setAntennaId(String antennaId) {
        this.antennaId = antennaId;
    }

    public void setTargetUid(String targetUid) {
        this.targetUid = targetUid;
    }

    @Override
    public String toString() {
        return targetUid + " " + antennaId;
    }
}
