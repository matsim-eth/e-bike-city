package ebikecity.utils;

import org.matsim.api.core.v01.network.Node;

public class NodeDistancePair implements Comparable<NodeDistancePair> {
    Node node;
    double distance;
    
    NodeDistancePair(Node node, double distance) {
        this.node = node;
        this.distance = distance;
    }
    
    @Override
    public int compareTo(NodeDistancePair other) {
        return Double.compare(this.distance, other.distance);
    }
}
