package ebikecity.utils.probabilisticrouting_dev;
import java.util.Objects;

import org.matsim.api.core.v01.Id;

class NodeLinkPair {
    private Id nodeId;
    private Id linkId;

    public NodeLinkPair(Id nodeId, Id linkId) {
        this.nodeId = nodeId;
        this.linkId = linkId;
    }

    public Id getNodeId() {
        return nodeId;
    }

    public Id getLinkId() {
        return linkId;
    }
    
    public boolean hasNullNodeId() {
        return nodeId == null;
    }

    public boolean hasNullLinkId() {
        return linkId == null;
    }

    @Override
    public String toString() {
        return "NodeId: " + (nodeId != null ? nodeId : "null") + ", LinkId: " + (linkId != null ? linkId : "null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NodeLinkPair that = (NodeLinkPair) obj;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(linkId, that.linkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, linkId);
    }
}
