package ebikecity.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;


public class MapFacilitiesBike {
	

	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {
		
		// load network, population and facilities
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);
				
		MatsimNetworkReader netReaderClassic = new MatsimNetworkReader(scenario.getNetwork());
		netReaderClassic.readFile(args[0]);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[1]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[2]);


				
		// filter all car links
		NetworkFilterManager n = new NetworkFilterManager(scenario.getNetwork());
		n.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAllowedModes().contains("car");
			}
		});
		
		// for network to map facilities available for car and  bike, remove highway and trunks from car network (n)
		Network nbc = n.applyFilters();
		for (Link link : nbc.getLinks().values()) {
			if ((link.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway")) ||
					(link.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk"))) {
				nbc.removeLink(link.getId());
				nbc.removeNode(link.getFromNode().getId());
				nbc.removeNode(link.getToNode().getId());
			}
		}
		for (Node node : nbc.getNodes().values()) {
			
			if ((node.getInLinks().isEmpty()) && (node.getOutLinks().isEmpty())) {
				nbc.removeNode(node.getId());
			}
		}
		
		
		// map the facilities
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
			
			// outside can / should stay where they are created during cutting
			Boolean isOutside = false;
			for (ActivityOption act : facility.getActivityOptions().values()) {
				if (act.getType().equals("outside")) {
					isOutside = true;
				}
				
			}
			if (!isOutside) {
				
								
				Link bikeableLink = NetworkUtils.getNearestRightEntryLink(nbc, facility.getCoord());
								
				((ActivityFacilityImpl) facility).setLinkId(bikeableLink.getId());
				
			}
			
		}
		
		// write facilities to file
		
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[4]);
		
	
				
		// map the activities in the population
		
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan removePlan = null;
			for (Plan plan : person.getPlans()) {
				if (!plan.equals(person.getSelectedPlan()))
					removePlan = plan;
			}
			if (removePlan != null) {
				person.getPlans().remove(removePlan);
			}
			
			
			// set link id of activities to link id from now mapped facility
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().toString() != "pt interaction") {
													
						Id<Link> linkId = scenario.getActivityFacilities().getFacilities().get(((Activity) pe).getFacilityId()).getLinkId();
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
		
		// swap facilities for bike trips from/to an outside facility to an outside facility on a bikeable link
		
		// collect all the facilities that are affected
		HashSet<Id<ActivityFacility>> swapFacilities = new HashSet<>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getAttributes().getAttribute("isOutside").toString().equals("true")) {
				Boolean usesBike = false;
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Leg) {
						if (((Leg) pe).getMode().toString() == "bike") {
							usesBike = true;
						}
					}
				}
				
				if (usesBike) {
					List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
					for (int i = 0; i < planElements.size(); i++) {
						PlanElement pe = planElements.get(i);
						if ((pe instanceof Leg) && (((Leg) pe).getMode().equals("bike"))) {
							PlanElement peb = planElements.get(i-1);
							if ((peb instanceof Activity) && (((Activity) peb).getType().toString().equals("outside"))) {
								swapFacilities.add(((Activity) peb).getFacilityId());							
							}
							PlanElement pea = planElements.get(i+1);
							if ((pea instanceof Activity) && (((Activity) pea).getType().toString().equals("outside"))) {
								swapFacilities.add(((Activity) pea).getFacilityId());							
							}
							
						}
					}
					
				}
			}
		}
		
		// create new facility on a bikeable link		
		HashMap<Id<ActivityFacility>,Id<ActivityFacility>> mapFacilities = new HashMap<>();
		
		ActivityFacilitiesFactory factory = scenario.getActivityFacilities().getFactory();
		ActivityOption actOptOutside = factory.createActivityOption("outside");
		
		for (Id<ActivityFacility> facId : swapFacilities) {
			
			ActivityFacility original = scenario.getActivityFacilities().getFacilities().get(facId);
			
			Id<ActivityFacility> newId = Id.create(facId.toString()+"_bike", ActivityFacility.class);
			Id<Link> newLinkId = NetworkUtils.getNearestRightEntryLink(nbc, original.getCoord()).getId();
			
			ActivityFacility bikeFac = factory.createActivityFacility(newId, original.getCoord(), newLinkId);
			bikeFac.addActivityOption(actOptOutside);
			
			scenario.getActivityFacilities().addActivityFacility(bikeFac);
			
			mapFacilities.put(facId,newId);
			
		}
		
		// swap facilities in the population
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getAttributes().getAttribute("isOutside").toString().equals("true")) {
				Boolean usesBike = false;
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Leg) {
						if (((Leg) pe).getMode().toString() == "bike") {
							usesBike = true;
						}
					}
				}
				
				if (usesBike) {
					List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
					for (int i = 0; i < planElements.size(); i++) {
						PlanElement pe = planElements.get(i);
						if ((pe instanceof Leg) && (((Leg) pe).getMode().equals("bike"))) {
							PlanElement peb = planElements.get(i-1);
							if ((peb instanceof Activity) && (((Activity) peb).getType().toString().equals("outside"))) {
								Id<ActivityFacility> newFacId = mapFacilities.get(((Activity) peb).getFacilityId());
								((Activity) peb).setFacilityId(newFacId);
								((Activity) peb).setLinkId(scenario.getActivityFacilities().getFacilities().get(newFacId).getLinkId());
							}
							PlanElement pea = planElements.get(i+1);
							if ((pea instanceof Activity) && (((Activity) pea).getType().toString().equals("outside"))) {
								Id<ActivityFacility> newFacId = mapFacilities.get(((Activity) pea).getFacilityId());
								((Activity) pea).setFacilityId(newFacId);
								((Activity) pea).setLinkId(scenario.getActivityFacilities().getFacilities().get(newFacId).getLinkId());							
							}
							
						}
					}
					
				}
			}
		}
		
		
		
		// write mapped population and complete facilities file
		
		new PopulationWriter(scenario.getPopulation()).write(args[3]);
		
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[4]);
		
		
	}
	
}
