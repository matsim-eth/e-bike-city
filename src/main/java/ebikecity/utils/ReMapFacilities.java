package ebikecity.utils;

// args
// [0]	input	plans from source simulation
// [1]	input	facilities from target simulation
// [2]	output	plans mapped for target simulation

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

// run this if you want to use a population in a new network where facilties have the same ids and
// links are in the same locations but have different ids

// args
// [0] plans (facilities with old ids)
// [1] facilities (with new oíds)
// [2] output plans (facilities with new ids)

public class ReMapFacilities {
	

	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {
		
		// scenario for classic input
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[1]);

				
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
													
						Id<Link> linkID = scenario.getActivityFacilities().getFacilities().get(((Activity) pe).getFacilityId()).getLinkId();
						((Activity) pe).setLinkId(linkID);
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
		
		// write mapped population
		
		new PopulationWriter(scenario.getPopulation()).write(args[2]);
			
	}
	
}
