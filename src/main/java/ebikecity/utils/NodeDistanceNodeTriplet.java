package ebikecity.utils;

import org.matsim.api.core.v01.Id;

public class NodeDistanceNodeTriplet implements Comparable<NodeDistanceNodeTriplet> {
    Id nodeId;
    double distance;
    Id previousNodeId;
    
    NodeDistanceNodeTriplet(Id nodeId, double distance, Id currentNodeId) {
        this.nodeId = nodeId;
        this.distance = distance;
        this.previousNodeId = currentNodeId;
    }
    
    @Override
    public int compareTo(NodeDistanceNodeTriplet other) {
        return Double.compare(this.distance, other.distance);
    }
}
