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


public class ManipulateNetwork {
	
	public static void reduceCapacity(Network net, Network filter, double factor) {
		for (Link link : filter.getLinks().values()) {
			net.removeLink(link.getId());
			Double capacity = link.getCapacity();
			link.setCapacity(capacity * factor);
			net.addLink(link);
		}
		
	}
	
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
		
		// args
		// [0] original network path
		// [1] new network path

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
		
		
		// example: reduce urban network capacity to 0.5 (s + t + u + r)
		double red = 0.7;
		reduceCapacity(n, ncs, red);
		reduceCapacity(n, nct, red);
		reduceCapacity(n, ncu, red);
		reduceCapacity(n, ncr, red);

			
		NetworkWriter netWriter = new NetworkWriter(n);
		netWriter.write(args[1]);
	
		
	}
}
