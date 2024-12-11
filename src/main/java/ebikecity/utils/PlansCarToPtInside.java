package ebikecity.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class PlansCarToPtInside {
	
	// rewrite all plans that are set to mode car to pt to start the simulation with emptier roads
	// change only inside agents, if mode choice for outside agents is supposed to be deactivated
	
	// args
	// [0] path to input plans (xml or xml.gz)
	// [1] path to output plans (xml or xml.gz)
	// [2] share of tours to be converted 0.0 ... 1.0

	public static void main(String[] args) throws IOException, InterruptedException {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
		// count plans that contain car
		double plans_count = 0.0;
		
		double change_factor = Double.parseDouble(args[2]);
		
		// store persons that have ony inside trips
		List<Id<Person>> poolIds = new ArrayList<Id<Person>>();	
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getAttributes().getAttribute("isOutside").equals(false)) {
				poolIds.add(person.getId());
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Leg) {
						if (((Leg) pe).getMode() == "car") {
							plans_count += 1.0;
							break;
						}
					}	
				}
			}
		}
		
		int stop = (int)(plans_count * change_factor);
		
//		System.out.println("All plans ----------------");
//		System.out.println(plans_count);
//		Thread.sleep(1500);
//		System.out.println("Stop ----------------");
//		System.out.println(stop);
//		Thread.sleep(1500);
		
		int plans_changed = 0;
		
		Collections.shuffle(poolIds);
		
		// instead of finding tours just change whole plans of person
		for (Id<Person> id : poolIds) {
			for (PlanElement pe : scenario.getPopulation().getPersons().get(id).getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode() == "car") {
						((Leg) pe).setMode("pt");
					}
				}
			}
			plans_changed += 1;
			if (plans_changed == stop) {
				break;
				}
		}

		new PopulationWriter(scenario.getPopulation()).write(args[1]);
	}
}

