package ebikecity.utils;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;

import ebikecity.project.config.AstraConfigGroup;
import ebikecity.project.config.AstraConfigurator;

public class CheckBikeAvailability {
	
	public static void main(String[] args) {
		
		// population
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("bikeAvailability", household.getAttributes().getAttribute("bikeAvailability"));
				}
			}
		}	
		
		
		// output plans
		Config configOut = ConfigUtils.loadConfig(args[1]);
		Scenario scenarioOut = ScenarioUtils.createScenario(configOut);
		ScenarioUtils.loadScenario(scenarioOut);
				
		// bikaAvailability
		// FOR_ALL
		// FOR_NONE
		// FOR_SOME
		
		int in_all = 0;
		int in_none = 0;
		int in_some = 0;
		
		int out_all = 0;
		int out_none = 0;
		int out_some = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
								
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_ALL")) {
					in_all++;			
				}
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
					in_none++;			
				}
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_SOME")) {
					in_some++;			
				}
			}
		}
		
		for (Person person : scenarioOut.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
				
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_ALL")) {
					out_all++;			
				}
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
					out_none++;			
				}
				if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_SOME")) {
					out_some++;			
				}
			}
		}
		
		System.out.println("Population bike availability for all:");
		System.out.println(in_all);
		
		System.out.println("Population bike availability for none:");
		System.out.println(in_none);
		
		System.out.println("Population bike availability for some:");
		System.out.println(in_some);
		
		System.out.println("Output population bike availability for all:");
		System.out.println(out_all);
		
		System.out.println("Output population bike availability for none:");
		System.out.println(out_none);
		
		System.out.println("Output population bike availability for some:");
		System.out.println(out_some);
		
		
	}
	
	
}
