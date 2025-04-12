import java.io.Serializable;
import java.util.ArrayList;

public class RoutingTable implements Serializable {
    private final ArrayList<Route> routes;

    public RoutingTable() {
        this.routes = new ArrayList<>();
    }

    public boolean contains(Route routeToCheck) {
        if (routeToCheck == null) return false;
        return routes.stream()
                .anyMatch(route -> route != null &&
                        route.getTargetUid().equals(routeToCheck.getTargetUid()));
    }

    public String findAntenna(String target) {
        return routes.stream()
                .filter(route -> target.equals(route.getTargetUid()))
                .findFirst()
                .map(Route::getAntennaId)
                .orElse(null);
    }

    public void addRoute(Route route) {
        this.routes.add(route);
    }

    public void deleteRoute(Route route) {
        this.routes.remove(route);
    }

    public void print() {
        for (Route route : routes) {
            System.out.println(route);
        }
    }

    public void updateAntenna(Route newRoute) {
        for (Route route : routes) {
            if (route.getTargetUid().equals(newRoute.getTargetUid())) {
                // Found the route - now update it
                route.setAntennaId(newRoute.getAntennaId());  // Requires setter in Route class
                return;  // Exit after updating
            }
        }
        // Optional: Handle case where targetUid wasn't found
        routes.add(newRoute);
    }
}
