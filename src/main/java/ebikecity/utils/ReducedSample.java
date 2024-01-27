package ebikecity.utils;

import java.io.IOException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

// create reduced sample of population

// args
// [0] input	file path 100% population
// [1] input 	file path network
// [2] output	file path x% population 
// [3] input	fraction (e.g. 0.1)

public class ReducedSample {

	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
				
		int pop_size = scenario.getPopulation().getPersons().keySet().size();
				
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[1]);
		
		Double fraction = Double.parseDouble(args[3]);
		
		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), fraction);
		popWriter.write(args[2]);
		
		Scenario control_scenario = ScenarioUtils.createMutableScenario(config);
		PopulationReader controlPopReader = new PopulationReader(control_scenario);
		controlPopReader.readFile(args[2]);
		
		int samp_size = control_scenario.getPopulation().getPersons().keySet().size();
				
		Double control = samp_size * 1.0 / pop_size;
		
		System.out.println("Population size --------");
		System.out.println(pop_size);
		System.out.println("Sample size ------------");
		System.out.println(samp_size);
		System.out.println("Control ----------------");
		System.out.println(control);
		System.out.println("Fraction ---------------");
		System.out.println(fraction);
		
	}
}
