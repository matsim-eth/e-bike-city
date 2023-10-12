package ebikecity.utils;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class ApplyNewNetworkTypes {
	
// class to map population/plans and facilities to a new network that was build from osm data
// modification of ApplyNewNetwork: filters take into account road type when higher level roads
// "classic" = cut out from the baseline switzerland scenario

// args
// [0] input path classic network
// [1] input path classic population
// [2] input path classic facilities
// [3] input path new network
// [4] output path mapped population
// [5] output path mapped facilities

	public static void main(String[] args) {

		// scenario for classic input
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimNetworkReader netReaderClassic = new MatsimNetworkReader(scenario.getNetwork());
		netReaderClassic.readFile(args[0]);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[1]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[2]);

		// scenario for new network
		Config configNew = ConfigUtils.createConfig();

		Scenario scenarioNew = ScenarioUtils.createMutableScenario(configNew);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenarioNew.getNetwork());
		netReader.readFile(args[3]);
		
		// set up link filter for new network
		
		// all car links
		NetworkFilterManager n = new NetworkFilterManager(scenarioNew.getNetwork());
		n.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAllowedModes().contains("car");
			}
		});
		
		// all motorway links
		NetworkFilterManager m = new NetworkFilterManager(scenarioNew.getNetwork());
		m.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway"); // cover motorway and motorway_link
			}
			
		});
		
		// all trunk links
		NetworkFilterManager t = new NetworkFilterManager(scenarioNew.getNetwork());
		t.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk"); // cover trunk and trunk_link
			}
			
		});
		
		// all primary links
		NetworkFilterManager p = new NetworkFilterManager(scenarioNew.getNetwork());
		p.addLinkFilter(new NetworkLinkFilter() {
			
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("primary"); // cover primary and primary_link
			}
			
		});
		
		
		// filtered subnetworks
		
		Network nn = n.applyFilters();
		
		Network nm = m.applyFilters();
		
		Network nt = t.applyFilters();
		
		Network np = p.applyFilters();
		

		// iterate over persons and assign activities to closest link on new (sub)network
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan removePlan = null;
			for (Plan plan : person.getPlans()) {
				if (!plan.equals(person.getSelectedPlan()))
					removePlan = plan;
			}
			if (removePlan != null) {
				person.getPlans().remove(removePlan);
			}

			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Coord coord = ((Activity) pe).getCoord();
					Id<Link> actLink = ((Activity) pe).getLinkId();
					// find actLink in classic scenario
					// if actLink is motorway use nm etc., else nn
					if (scenario.getNetwork().getLinks().get(actLink).getAttributes().getAttribute("osm:way:highway") == null) {
						Id<Link> linkId = NetworkUtils.getNearestLinkExactly(nn, coord).getId();
						((Activity) pe).setLinkId(linkId);
					}
					else if (scenario.getNetwork().getLinks().get(actLink).getAttributes().getAttribute("osm:way:highway").toString().contains("motorway")) {
						Id<Link> linkId = NetworkUtils.getNearestLinkExactly(nm, coord).getId();
						((Activity) pe).setLinkId(linkId);	
					}
					else if (scenario.getNetwork().getLinks().get(actLink).getAttributes().getAttribute("osm:way:highway").toString().contains("trunk")) {
						Id<Link> linkId = NetworkUtils.getNearestLinkExactly(nt, coord).getId();
						((Activity) pe).setLinkId(linkId);
					}
					else if (scenario.getNetwork().getLinks().get(actLink).getAttributes().getAttribute("osm:way:highway").toString().contains("primary")) {
						Id<Link> linkId = NetworkUtils.getNearestLinkExactly(np, coord).getId();
						((Activity) pe).setLinkId(linkId);
					}
					else {
						Id<Link> linkId = NetworkUtils.getNearestLinkExactly(nn, coord).getId();
						((Activity) pe).setLinkId(linkId);
					}	
				}
			}

			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
			for (Trip trip : trips) {
				for (Leg leg : trip.getLegsOnly()) {
					leg.setRoute(null);

				}
			}
		}

		// iterate over facilities and assign activities to closest link on new (sub)network
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {

			Coord coord = facility.getCoord();
			Id<Link> facLink = facility.getLinkId();
			
			if (scenario.getNetwork().getLinks().get(facLink).getAttributes().getAttribute("osm:way:highway").toString().contains("motorway")) {
				((ActivityFacilityImpl) facility)
					.setLinkId(NetworkUtils.getNearestLinkExactly(nm, coord).getId());
			}
			else if (scenario.getNetwork().getLinks().get(facLink).getAttributes().getAttribute("osm:way:highway").toString().contains("trunk")) {
				((ActivityFacilityImpl) facility)
					.setLinkId(NetworkUtils.getNearestLinkExactly(nt, coord).getId());
			}
			else if (scenario.getNetwork().getLinks().get(facLink).getAttributes().getAttribute("osm:way:highway").toString().contains("primary")) {
				((ActivityFacilityImpl) facility)
					.setLinkId(NetworkUtils.getNearestLinkExactly(np, coord).getId());
			}
			else {
			((ActivityFacilityImpl) facility)
					.setLinkId(NetworkUtils.getNearestLinkExactly(nn, coord).getId());
			}

		}
		
		// write mapped population and facilities file
		new PopulationWriter(scenario.getPopulation()).write(args[4]);
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[5]);
	}
}
