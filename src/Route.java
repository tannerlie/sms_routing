public class Route {
    private String targetUid;
    private String antennaId;

    public Route(String targetUid, String antenna) {
        this.targetUid = targetUid;
        this.antennaId = antenna;
    }
    public String getTargetUid() {
        return targetUid;
    }

    public String getAntenna() {
        return antennaId;
    }
}
