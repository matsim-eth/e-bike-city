package ebikecity.utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

// create reduced sample of population that contains the same agents as another reduced sample

// args
// [0] input	file path 100% population
// [1] input 	file path network
// [2] input	file path x% reference population
// [3] output	file path x% population

public class SameSample {

	public static void main(String[] args) throws IOException {

		// scenario for population you want to cut
		
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
//		int pop_size = scenario.getPopulation().getPersons().keySet().size();
				
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[1]);
		
		// scenario for reference sample population
		
		Config refConfig = ConfigUtils.createConfig();

		Scenario refScenario = ScenarioUtils.createMutableScenario(refConfig);

		PopulationReader refPopReader = new PopulationReader(refScenario);
		refPopReader.readFile(args[2]);
		
		Set<Id<Person>> refPersons;
		
		refPersons = refScenario.getPopulation().getPersons().keySet();
				
		// remove agents that are not in reference population
		
		Set<Id<Person>> removePersons = new HashSet<Id<Person>>();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if(!refPersons.contains(person.getId())) {
				removePersons.add(person.getId());
			}
			
		}
		
		for (Id<Person> remove : removePersons) {
			scenario.getPopulation().removePerson(remove);
		}
	
		
		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popWriter.write(args[2]);
		
		Scenario control_scenario = ScenarioUtils.createMutableScenario(config);
		PopulationReader controlPopReader = new PopulationReader(control_scenario);
		controlPopReader.readFile(args[3]);
		
//		int samp_size = control_scenario.getPopulation().getPersons().keySet().size();
//				
//		Double control = samp_size * 1.0 / pop_size;
//		
//		System.out.println("Population size --------");
//		System.out.println(pop_size);
//		System.out.println("Sample size ------------");
//		System.out.println(samp_size);
//		System.out.println("Control ----------------");
//		System.out.println(control);
		
	}
}
