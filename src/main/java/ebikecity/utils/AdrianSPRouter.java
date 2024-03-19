package ebikecity.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.facilities.Facility;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class AdrianSPRouter {

	private final Network network;
	private final NetworkRoutingModule router;

	public AdrianSPRouter(Network network, String configPath, String eventsFilename) {
		this.network = network;

		Config config = ConfigUtils.loadConfig(configPath);

		DijkstraFactory factory = new DijkstraFactory();

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.configure(config.travelTimeCalculator());
		TravelTimeCalculator ttc = builder.build();

		TravelTime tt = ttc.getLinkTravelTimes();

		TravelDisutility td = new OnlyTimeDependentTravelDisutility(tt);

		LeastCostPathCalculator routeAlgo = factory.createPathCalculator(network, td, tt);
		this.router = new NetworkRoutingModule(TransportMode.bike, PopulationUtils.getFactory(), network, routeAlgo);
	}

	public void run(String relationsCsvPath, String outputPath) {

		try {
			CSVWriter writer = new CSVWriter(new FileWriter(outputPath), ';', CSVWriter.NO_QUOTE_CHARACTER);
			String[] columns = { "id", "networkdistance", "traveltime", "links" };
			writer.writeNext(columns);
			CSVReader reader = new CSVReader(new FileReader(relationsCsvPath), ';');

			reader.readNext();
			String[] arr;
			while ((arr = reader.readNext()) != null) {
				if (arr.length > 0) {
					// only read lines that have stuff, and ignore empty lines
					String id = arr[0];
					float fromX = Float.parseFloat(arr[1]);
					float fromY = Float.parseFloat(arr[2]);
					float toX = Float.parseFloat(arr[3]);
					float toY = Float.parseFloat(arr[4]);
					double startTime = Double.parseDouble(arr[5]);
					Leg leg = this.fetch(fromX, fromY, toX, toY, startTime);
					List<Id<Link>> linkIds = ((NetworkRoute)leg.getRoute()).getLinkIds();
					String links = new String("");
					boolean start = true;
					for (Id<Link> linkid : linkIds) {
						if (!start) 
							links.concat(",");
						else
							start = false;
						links.concat(linkid.toString());
						
					}
					String[] toWrite = { id, Double.toString(leg.getRoute().getDistance()),
							Double.toString(leg.getRoute().getTravelTime().seconds()), links };
					writer.writeNext(toWrite);
				}

			}
			writer.flush();
			writer.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Leg fetch(float fromX, float fromY, float toX, float toY, double startTime) {

		Activity fromAct = PopulationUtils.createActivityFromCoord("h", new Coord(fromX, fromY));
		Facility fromFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(this.network, fromAct.getCoord()));

		Activity toAct = PopulationUtils.createActivityFromCoord("h", new Coord(toX, toY));
		Facility toFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(this.network, toAct.getCoord()));

		List<? extends PlanElement> pes = this.router.calcRoute(fromFacility, toFacility, startTime, null);
		return (Leg) pes.get(0);
	}

	public static void main(String[] args) {
		// folder where the MATSim files is stored
		String runInputFolder = args[0];
		// true: use congested times
		boolean calcCongestedTravelTimes = false;
		// file containing routes to route with columns [id, fromx, fromy, tox, toy,
		// departuretime]
		String odsFile = args[1];
		// file where to store the routed ODs
		String outputFile = args[2];

		// read network file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(runInputFolder + "/" + "network.xml.gz");
		Network reducedNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(reducedNetwork, CollectionUtils.stringToSet(TransportMode.bike));

		AdrianSPRouter ods;
		if (calcCongestedTravelTimes) {
			String config = runInputFolder + "/" + "output_config.xml";
			String events = runInputFolder + "/" + "output_events.xml.gz";
			ods = new AdrianSPRouter(reducedNetwork, config, events);
		} else {
			// TODO: routing with freeflow travel times is not yet implemented!!
			String config = runInputFolder + "/" + "config.xml";
			ods = new AdrianSPRouter(reducedNetwork, config, null);
		}
		ods.run(odsFile, outputFile);

	}

}