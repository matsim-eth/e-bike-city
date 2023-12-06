package ebikecity.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class WriteBikeNetwork {
	
	public static void main(String[] args) {
		
		// load network, population and facilities
				Config config = ConfigUtils.createConfig();

				Scenario scenario = ScenarioUtils.createMutableScenario(config);
						
				MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
				netReader.readFile(args[0]);
				
				Network bikeNet = NetworkUtils.createNetwork();
				
				Set<String> modes = new HashSet<String>();
				modes.add("bike");
				
				new TransportModeNetworkFilter(scenario.getNetwork()).filter(bikeNet, modes);
				
				new NetworkWriter(bikeNet).write(args[1]);

	}

}
