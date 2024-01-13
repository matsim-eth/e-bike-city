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

public class OutsidePopulationFilter {
	
	// rewrite all plans that are set to mode car to walk to start the simulation with empty roads
	// change only inside agents, if mode choice for outside agents is supposed to be deactivated

	public static void main(String[] args) throws IOException, InterruptedException {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
		// count plans that contain car
		
		List<Id<Person>> inIds = new ArrayList<Id<Person>>();
		
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (person.getAttributes().getAttribute("isOutside").equals(false)) {
				inIds.add(person.getId());
			}
		}
		
		for (Id<Person> id : inIds) {
			scenario.getPopulation().removePerson(id);
		}	
		
		new PopulationWriter(scenario.getPopulation()).write(args[1]);
	}
}

