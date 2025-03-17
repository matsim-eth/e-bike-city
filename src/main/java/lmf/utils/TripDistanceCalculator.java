package lmf.utils;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

public class TripDistanceCalculator {

    private Network network;
    private Geometry boundary;
    // Aggregated total distance (in meters) by mode.
    private Map<String, Double> totalDistanceByMode = new HashMap<>();

    /**
     * Constructor.
     * Reads the shapefile and network file, prints coordinate system and boundaries,
     * and then adjusts the network so that only links (or the in-boundary portions)
     * within the shapefile polygon are kept.
     *
     * @param shapefilePath  path to the ESRI shapefile
     * @param networkPath    path to the MATSim network file
     * @throws Exception if reading files fails
     */
    public TripDistanceCalculator(String shapefilePath, String networkPath) throws Exception {
        this.boundary = loadBoundaryFromShapefile(shapefilePath);
        this.network = readNetwork(networkPath);
        adjustNetwork(this.network, this.boundary);
    }

    /**
     * Loads the shapefile and returns a unioned Geometry representing the boundary.
     * Also prints the shapefile CRS and overall boundary.
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
        SimpleFeatureType schema = source.getSchema();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        System.out.println("Shapefile CRS: " + (crs != null ? crs.toString() : "undefined"));
        
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
     * Reads the MATSim network file and prints its boundary.
     */
    private Network readNetwork(String networkPath) {
        Network net = NetworkUtils.createNetwork();
        new MatsimNetworkReader(net).readFile(networkPath);
        
        // Compute network boundary from node coordinates.
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Node node : net.getNodes().values()) {
            Coord coord = node.getCoord();
            if (coord.getX() < minX) { minX = coord.getX(); }
            if (coord.getX() > maxX) { maxX = coord.getX(); }
            if (coord.getY() < minY) { minY = coord.getY(); }
            if (coord.getY() > maxY) { maxY = coord.getY(); }
        }
        System.out.println("Network boundary: [" + minX + ", " + minY + "] - [" + maxX + ", " + maxY + "]");
        
        return net;
    }

    /**
     * Adjusts the network links so that only the parts inside the provided polygon are kept.
     * Links completely outside the polygon are removed; links that cross the polygon boundary are trimmed
     * (their length is updated to the length of the intersection).
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
            
            // Determine if the endpoints are inside the polygon.
            boolean fromInside = polygon.contains(gf.createPoint(fromCoord));
            boolean toInside = polygon.contains(gf.createPoint(toCoord));
            
            if (fromInside && toInside) {
                // Entire link is inside the polygon; no adjustment needed.
                continue;
            } else {
                // Compute the intersection of the link with the polygon.
                Geometry intersection = linkLine.intersection(polygon);
                double insideLength = intersection.getLength();
                
                // If the intersection has effectively zero length, the link is considered outside.
                if (insideLength < 1e-6) {
                    linksToRemove.add(link.getId());
                } else {
                    // Adjust the link's length to the portion inside the polygon.
                    link.setLength(insideLength);
                }
            }
        }
        
        // Remove links that are completely outside the polygon.
        for (Id<Link> linkId : linksToRemove) {
            network.removeLink(linkId);
        }
    }

    /**
     * Processes the MATSim events file.
     * A combined event handler is created that listens for PersonDepartureEvent, LinkEnterEvent,
     * and PersonArrivalEvent.
     *
     * For each trip, when a person departs the mode is recorded, the adjusted link lengths (from the network)
     * are summed as the person enters links, and on arrival the trip’s total distance is added to the overall
     * sum (aggregated by mode).
     *
     * @param eventsFilePath path to the MATSim events file
     */
    public void processEvents(String eventsFilePath) {
        // Use EventsManagerImpl directly.
        EventsManagerImpl eventsManager = new EventsManagerImpl();
        TripDistanceEventHandler handler = new TripDistanceEventHandler();
        eventsManager.addHandler(handler);
        new MatsimEventsReader(eventsManager).readFile(eventsFilePath);
        // Retrieve the aggregated result.
        this.totalDistanceByMode = handler.getTotalDistanceByMode();
    }
   

    /**
     * Writes the aggregated person-km by mode to a CSV file.
     *
     * @param outputCsvPath path to the output CSV file.
     * @throws Exception if writing fails.
     */
    public void writeCsv(String outputCsvPath) throws Exception {
        try (PrintWriter writer = new PrintWriter(new File(outputCsvPath))) {
            writer.println("mode,person_km");
            for (Map.Entry<String, Double> entry : totalDistanceByMode.entrySet()) {
                // Convert meters to kilometers.
                double km = entry.getValue() / 1000.0;
                writer.println(entry.getKey() + "," + km);
            }
        }
    }

    /**
     * Combined event handler that implements BasicEventHandler.
     * It handles:
     * - PersonDepartureEvent: records the trip’s mode and resets the accumulated distance.
     * - LinkEnterEvent: adds the (adjusted) link length to the person’s current trip distance.
     * - PersonArrivalEvent: finalizes the trip and aggregates the trip distance (in meters) by mode.
     */
private class TripDistanceEventHandler implements BasicEventHandler {

    // Accumulated distance (in meters) per person for the current trip.
    private final Map<Id, Double> currentTripDistance = new HashMap<>();
    // The mode of the current trip per person.
    private final Map<Id, String> currentTripMode = new HashMap<>();
    // Total distance (in meters) aggregated by mode.
    private final Map<String, Double> totalDistanceByMode = new HashMap<>();
    // Mapping from vehicle ID to person ID (for modes that produce LinkEnterEvent).
    private final Map<Id, Id> vehicleToPerson = new HashMap<>();

    @Override
    public void handleEvent(Event event) {
        if (event instanceof PersonDepartureEvent) {
            PersonDepartureEvent dep = (PersonDepartureEvent) event;
            // Initialize a new trip for this person:
            currentTripDistance.put(dep.getPersonId(), 0.0);
            currentTripMode.put(dep.getPersonId(), dep.getLegMode());

        } else if (event instanceof PersonEntersVehicleEvent) {
            // For car, bike, etc. we need to map vehicle ID -> person ID.
            PersonEntersVehicleEvent pe = (PersonEntersVehicleEvent) event;
            vehicleToPerson.put(pe.getVehicleId(), pe.getPersonId());
            
            

        } else if (event instanceof LinkEnterEvent) {
            // Network-based mode (car, bike, etc.) => accumulate link distance.
            LinkEnterEvent linkEvent = (LinkEnterEvent) event;
            Id vehicleId = linkEvent.getVehicleId();
            Id personId = vehicleToPerson.get(vehicleId);
            // Fallback if no mapping:
            if (personId == null) {
                personId = vehicleId;
            }
            if (personId != null && currentTripDistance.containsKey(personId)) {
                Link link = network.getLinks().get(linkEvent.getLinkId());
                if (link != null) {
                    double length = link.getLength();
                    double current = currentTripDistance.get(personId);
                    currentTripDistance.put(personId, current + length);
                }
            }

        } else if (event instanceof PersonArrivalEvent) {
            PersonArrivalEvent arr = (PersonArrivalEvent) event;
            Id personId = arr.getPersonId();
            if (currentTripDistance.containsKey(personId)) {
                double tripDistance = currentTripDistance.get(personId);

                // For teleported train or other teleported modes, look for a distance attribute:
                String distStr = event.getAttributes().get("legDistance");
                if (distStr != null) {
                	System.out.println("Teleported Distance is "+tripDistance);
                    double teleportedDist = Double.parseDouble(distStr);
                    tripDistance += teleportedDist;
                }

                // Aggregate by mode:
                String mode = currentTripMode.get(personId); // Should be "train" or something
                double currentTotal = totalDistanceByMode.getOrDefault(mode, 0.0);
                totalDistanceByMode.put(mode, currentTotal + tripDistance);

                // Clear the trip data:
                currentTripDistance.remove(personId);
                currentTripMode.remove(personId);
            }
        }
    }

    @Override
    public void reset(int iteration) {
        // Not needed in this context.
    }

    public Map<String, Double> getTotalDistanceByMode() {
        return totalDistanceByMode;
    }
}
    /**
     * Main method.
     *
     * Expected arguments:
     *  args[0] - Path to the shapefile.
     *  args[1] - Path to the MATSim network file.
     *  args[2] - Path to the MATSim events file.
     *  args[3] - Output CSV file path.
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java TripDistanceCalculator <shapefilePath> <networkFilePath> <eventsFilePath> <outputCsvPath>");
            return;
        }
        String shapefilePath = args[0];
        String networkFilePath = args[1];
        String eventsFilePath = args[2];
        String outputCsvPath = args[3];

        try {
            TripDistanceCalculator calculator = new TripDistanceCalculator(shapefilePath, networkFilePath);
            calculator.processEvents(eventsFilePath);
            calculator.writeCsv(outputCsvPath);
            System.out.println("CSV file written to " + outputCsvPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
