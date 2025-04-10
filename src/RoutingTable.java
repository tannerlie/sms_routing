import java.util.ArrayList;

public class RoutingTable {
    private ArrayList<Route> routes;

    public RoutingTable() {
        this.routes = new ArrayList<>();
    }

    public boolean contains(Route route) {
        return routes.contains(route);
    }

    public void addRoute(Route route) {
        this.routes.add(route);
    }

    public void deleteRoute(Route route) {
        this.routes.remove(route);
    }
}
