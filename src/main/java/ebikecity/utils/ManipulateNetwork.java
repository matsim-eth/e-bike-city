package ebikecity.utils;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

// run this to reduce network capacity of number of lanes on certain links of the network
// by filtering by road type

// you might have to change something in the code but it should run on a local computer
// so no need to pack every time and upload on Euler

// args
// [0] original network path (xml or xml.gz)
// [1] new network path (xml or xml.gz)

public class ManipulateNetwork {
	
	
	// method to switch out link in network with version with scaled capacity 
	public static void reduceCapacity(Network net, Network filter, double factor) {
		for (Link link : filter.getLinks().values()) {
			net.removeLink(link.getId());
			Double capacity = link.getCapacity();
			link.setCapacity(capacity * factor);
			net.addLink(link);
		}
		
	}
	
	// method to switch out link in network with version with reduced lanes 
	public static void removeLane(Network net, Network filter, double nlanes) {
		for (Link link : filter.getLinks().values()) {
			net.removeLink(link.getId());
			Double num_lanes = link.getNumberOfLanes();
			// System.out.println(num_lanes.toString());
			if (num_lanes > nlanes) {
				link.setNumberOfLanes(num_lanes - nlanes);
			}
			net.addLink(link);
			
		}
	}

	public static void main(String[] args) {

		// load network
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[0]);
		
		
		// complete network
		Network n = scenario.getNetwork();
		
		
		// filtered networks
		
		// car links
		NetworkFilterManager c = new NetworkFilterManager(scenario.getNetwork());
		c.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAllowedModes().contains("car");
			}
			
		});
			
				
		Network nc = c.applyFilters();
		
		// motorway links
		NetworkFilterManager m = new NetworkFilterManager(nc);
		m.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway");
			}
		});
		
		Network ncm = m.applyFilters();
		
		// trunk links
		NetworkFilterManager tr = new NetworkFilterManager(nc);
		tr.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk");
			}
		});
		
		Network nctr = tr.applyFilters();
		
		// primary links
		NetworkFilterManager p = new NetworkFilterManager(nc);
		p.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("primary");
			}
		});
		
		Network ncp= p.applyFilters();
		
		// secondary links
		NetworkFilterManager s = new NetworkFilterManager(nc);
		s.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("secondary");
			}
		});
		
		Network ncs = s.applyFilters();
		
		// tertiary links
		NetworkFilterManager t = new NetworkFilterManager(nc);
		t.addLinkFilter(new NetworkLinkFilter() {
				
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("tertiary");
			}
		});
				
		Network nct = t.applyFilters();
		
		// unclassified links
		NetworkFilterManager u = new NetworkFilterManager(nc);
		u.addLinkFilter(new NetworkLinkFilter() {
				
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("unclassified");
			}
		});
				
		Network ncu = u.applyFilters();
		
		// residential links
		NetworkFilterManager r = new NetworkFilterManager(nc);
		r.addLinkFilter(new NetworkLinkFilter() {
				
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("residential");
			}
		});
				
		Network ncr = r.applyFilters();
		
		
		// example: reduce urban network capacity to 0.9 (s + t + u + r)
//		double red = 0.9;
//		reduceCapacity(n, ncs, red);
//		reduceCapacity(n, nct, red);
//		reduceCapacity(n, ncu, red);
//		reduceCapacity(n, ncr, red);
		
		// example: reduce all car links by 0.25
		double red = 0.25;
		reduceCapacity(n, nc, red);
		
		// example: reduce one land for all secondary links (with more than one lane)
//		int nlanes = 1;
//		removeLane(n, ncs, nlanes);

		// write result	
		NetworkWriter netWriter = new NetworkWriter(n);
		netWriter.write(args[1]);
	
		
	}
}
