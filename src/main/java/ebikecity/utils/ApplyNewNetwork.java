package ebikecity.utils;

import java.util.List;

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.network.NetworkUtils;
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

public class ApplyNewNetwork {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[1]);

		Config configNew = ConfigUtils.createConfig();

		Scenario scenarioNew = ScenarioUtils.createMutableScenario(configNew);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenarioNew.getNetwork());
		netReader.readFile(args[2]);

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
					Id<Link> linkId = NetworkUtils.getNearestLinkExactly(scenarioNew.getNetwork(), coord).getId();
					((Activity) pe).setLinkId(linkId);
				}
			}

			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
			for (Trip trip : trips) {
				for (Leg leg : trip.getLegsOnly()) {
					leg.setRoute(null);

				}
			}
		}

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {

			Coord coord = facility.getCoord();

			((ActivityFacilityImpl) facility)
					.setLinkId(NetworkUtils.getNearestLinkExactly(scenarioNew.getNetwork(), coord).getId());

		}
		
		new PopulationWriter(scenario.getPopulation()).write(args[3]);
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[4]);
	}
}
