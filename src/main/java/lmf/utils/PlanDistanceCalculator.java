package lmf.utils;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.NoSuchElementException;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PlanDistanceCalculator {

    private final Scenario scenario;
    private final Geometry boundary;
    private final Network network;

    // Store total inside-distance by mode
    private final Map<String, Double> distanceByMode = new HashMap<>();

    public PlanDistanceCalculator(String shapefilePath, String networkPath) throws Exception {
        // 1) Create a minimal scenario (no config file needed).
        this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        // 2) Load the shapefile boundary
        this.boundary = loadBoundaryFromShapefile(shapefilePath);

        // 3) Load the network
        this.network = this.scenario.getNetwork();
        new MatsimNetworkReader(this.network).readFile(networkPath);

        // 4) Trim the network to the shapefile boundary
        adjustNetwork(this.network, this.boundary);
    }

    /**
     * Loads the shapefile and returns a unioned Geometry representing the boundary.
     */
    private Geometry loadBoundaryFromShapefile(String shapefilePath) throws Exception {
        File file = new File(shapefilePath);
        URL fileURL = file.toURI().toURL();
        Map<String, Object> map = new HashMap<>();
        map.put("url", fileURL);

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        if (dataStore == null) {
            throw new NoSuchElementException("Could not open shapefile " + shapefilePath);
        }

        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
        SimpleFeatureCollection collection = (SimpleFeatureCollection) source.getFeatures();

        Geometry unionGeometry = null;
        try (SimpleFeatureIterator features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                if (unionGeometry == null) {
                    unionGeometry = geom;
                } else {
                    unionGeometry = unionGeometry.union(geom);
                }
            }
        }
        dataStore.dispose();

        Envelope env = unionGeometry.getEnvelopeInternal();
        System.out.println("Shapefile boundary: " + env);
        return unionGeometry;
    }

    /**
     * Adjust the network so that only the parts inside the polygon remain.
     * Links crossing the boundary are trimmed.
     */
    private void adjustNetwork(Network network, Geometry polygon) {
        GeometryFactory gf = new GeometryFactory();
        List<Id<Link>> linksToRemove = new ArrayList<>();

        for (Link link : network.getLinks().values()) {
            Coord from = link.getFromNode().getCoord();
            Coord to = link.getToNode().getCoord();
            Coordinate fromCoord = new Coordinate(from.getX(), from.getY());
            Coordinate toCoord = new Coordinate(to.getX(), to.getY());
            LineString linkLine = gf.createLineString(new Coordinate[]{fromCoord, toCoord});

            Geometry intersection = linkLine.intersection(polygon);
            double insideLength = intersection.getLength();

            if (insideLength < 1e-6) {
                // effectively outside
                linksToRemove.add(link.getId());
            } else if (Math.abs(insideLength - link.getLength()) > 1e-6) {
                // partial intersection
                link.setLength(insideLength);
            }
        }

        // remove links fully outside
        for (Id<Link> linkId : linksToRemove) {
            network.removeLink(linkId);
        }
    }

    /**
     * Reads the plan file, then iterates over persons and selected plans to compute
     * the distance inside the boundary by mode.
     */
    public void processPlans(String plansFile) {
        // Read population (plans)
        PopulationReader popReader = new PopulationReader(this.scenario);
        popReader.readFile(plansFile);

        // For each person, for each *selected* plan, parse the legs
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan selectedPlan = person.getSelectedPlan();
            if (selectedPlan == null) continue;

            List<PlanElement> planElements = selectedPlan.getPlanElements();
            for (PlanElement pe : planElements) {
                if (pe instanceof Leg) {
                    Leg leg = (Leg) pe;
                    String mode = leg.getMode();
                    double insideDistance = 0.0;

                    // 1) If mode is "car" or "car_passenger"
                    if ("car".equals(mode) || "car_passenger".equals(mode)) {
                        if (leg.getRoute() instanceof NetworkRoute) {
                            NetworkRoute netRoute = (NetworkRoute) leg.getRoute();
                            insideDistance = sumLinkDistances(netRoute);
                        } else {
                            // fallback
                            insideDistance = leg.getRoute().getDistance();
                        }
                    }
                    // 2) If mode is "walk"
                    else if ("walk".equals(mode)) {
                        if (leg.getRoute() instanceof GenericRouteImpl) {
                            GenericRouteImpl gRoute = (GenericRouteImpl) leg.getRoute();
                            Id<Link> startId = gRoute.getStartLinkId();
                            Id<Link> endId = gRoute.getEndLinkId();
                            if (isLinkInside(startId) || isLinkInside(endId)) {
                                insideDistance = gRoute.getDistance();
                            }
                        } else {
                            // fallback
                            insideDistance = leg.getRoute().getDistance();
                        }
                    }
                    // 3) If mode is "pt"
                    else if ("pt".equals(mode)) {
                        if (leg.getRoute() instanceof GenericRouteImpl) {
                            GenericRouteImpl gRoute = (GenericRouteImpl) leg.getRoute();
                            Id<Link> startId = gRoute.getStartLinkId();
                            Id<Link> endId = gRoute.getEndLinkId();
                            boolean startInside = isLinkInside(startId);
                            boolean endInside = isLinkInside(endId);
                            double dist = gRoute.getDistance();

                            if (startInside && endInside) {
                                insideDistance = dist;
                            } else if (startInside ^ endInside) {
                                // partial
                            	// 1) Build a line from the network link coordinates:
                            	Coord startCoord = network.getLinks().get(startId).getToNode().getCoord();
                            	Coord endCoord = network.getLinks().get(endId).getToNode().getCoord();

                            	// (If your links are reversed or you prefer fromNode coords, adjust accordingly.)

                            	// 2) Create a JTS LineString
                            	GeometryFactory gf = new GeometryFactory();
                            	LineString routeLine = gf.createLineString(new Coordinate[]{
                            	    new Coordinate(startCoord.getX(), startCoord.getY()),
                            	    new Coordinate(endCoord.getX(), endCoord.getY())
                            	});

                            	// 3) Compute the fraction inside the polygon
                            	double routeLineLen = routeLine.getLength();
                            	Geometry intersection = routeLine.intersection(boundary);
                            	double insideLen = intersection.getLength();

                            	// Edge case check: if routeLineLen is zero or intersection is empty
                            	if (routeLineLen < 1e-6 || insideLen < 1e-6) {
                            	    insideDistance = 0.0;
                            	} else {
                            	    double fractionInside = insideLen / routeLineLen;
                            	    // 4) Multiply total route distance by that fraction
                            	    insideDistance = dist * fractionInside;
                            	}                            }
                            // else 0.0
                        } else if (leg.getRoute() instanceof TransitPassengerRoute) {
                            TransitPassengerRoute tpr = (TransitPassengerRoute) leg.getRoute();
                            Id<Link> startId = tpr.getStartLinkId();
                            Id<Link> endId = tpr.getEndLinkId();
                            double dist = tpr.getDistance();
                            boolean startInside = isLinkInside(startId);
                            boolean endInside = isLinkInside(endId);

                            if (startInside && endInside) {
                                insideDistance = dist;
                            } else if (startInside ^ endInside) {
                                // partial
                            	// 1) Build a line from the network link coordinates:
                            	Coord startCoord = network.getLinks().get(startId).getToNode().getCoord();
                            	Coord endCoord = network.getLinks().get(endId).getToNode().getCoord();

                            	// (If your links are reversed or you prefer fromNode coords, adjust accordingly.)

                            	// 2) Create a JTS LineString
                            	GeometryFactory gf = new GeometryFactory();
                            	LineString routeLine = gf.createLineString(new Coordinate[]{
                            	    new Coordinate(startCoord.getX(), startCoord.getY()),
                            	    new Coordinate(endCoord.getX(), endCoord.getY())
                            	});

                            	// 3) Compute the fraction inside the polygon
                            	double routeLineLen = routeLine.getLength();
                            	Geometry intersection = routeLine.intersection(boundary);
                            	double insideLen = intersection.getLength();

                            	// Edge case check: if routeLineLen is zero or intersection is empty
                            	if (routeLineLen < 1e-6 || insideLen < 1e-6) {
                            	    insideDistance = 0.0;
                            	} else {
                            	    double fractionInside = insideLen / routeLineLen;
                            	    // 4) Multiply total route distance by that fraction
                            	    insideDistance = dist * fractionInside;
                            	}                            }
                        } else {
                            insideDistance = leg.getRoute().getDistance();
                        }
                    }
                    // 4) Otherwise (bike, truck, etc.)
                    else {
                        if (leg.getRoute() instanceof NetworkRoute) {
                            insideDistance = sumLinkDistances((NetworkRoute) leg.getRoute());
                        } else {
                            insideDistance = leg.getRoute().getDistance();
                        }
                    }

                    // Accumulate
                    distanceByMode.put(mode, distanceByMode.getOrDefault(mode, 0.0) + insideDistance);
                }
            }
        }
    }

    /**
     * Sums the lengths of the link IDs in the given route, using the trimmed network.
     */
    private double sumLinkDistances(NetworkRoute netRoute) {
        double sum = 0.0;
        // Add startLink if needed
        Id<Link> start = netRoute.getStartLinkId();
        sum += getLinkLengthIfExists(start);

        // Middle link IDs
        for (Id<Link> linkId : netRoute.getLinkIds()) {
            sum += getLinkLengthIfExists(linkId);
        }

        // Add endLink
        Id<Link> end = netRoute.getEndLinkId();
        sum += getLinkLengthIfExists(end);

        return sum;
    }

    /**
     * Returns link length if the link is in the network; else 0.
     */
    private double getLinkLengthIfExists(Id<Link> linkId) {
        Link link = this.network.getLinks().get(linkId);
        return (link != null) ? link.getLength() : 0.0;
    }

    /**
     * Quick check if the link is inside the trimmed network (i.e., still exists).
     */
    private boolean isLinkInside(Id<Link> linkId) {
        return this.network.getLinks().containsKey(linkId);
    }

    /**
     * Writes the aggregated person-km by mode to a CSV file.
     */
    public void writeCsv(String outputCsvPath) throws Exception {
        try (PrintWriter writer = new PrintWriter(new File(outputCsvPath))) {
            writer.println("mode,person_km");
            for (Map.Entry<String, Double> entry : distanceByMode.entrySet()) {
                double km = entry.getValue() / 1000.0;
                writer.println(entry.getKey() + "," + km);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java PlanDistanceCalculator <shapefilePath> <networkFilePath> <plansFile> <outputCsvPath>");
            return;
        }
        String shapefilePath = args[0];
        String networkFilePath = args[1];
        String plansFile = args[2];
        String outputCsvPath = args[3];

        try {
            PlanDistanceCalculator calculator = new PlanDistanceCalculator(shapefilePath, networkFilePath);
            calculator.processPlans(plansFile);
            calculator.writeCsv(outputCsvPath);
            System.out.println("CSV file written to " + outputCsvPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
