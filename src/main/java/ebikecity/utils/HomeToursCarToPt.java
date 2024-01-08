package ebikecity.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class HomeToursCarToPt {
	
	// rewrite all plans that are set to mode car to walk to start the simulation with empty roads
	// change only inside agents, if mode choice for outside agents is supposed to be deactivated

	public static void main(String[] args) throws IOException, InterruptedException {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
		// count tours that contain car
		int tours_count = 0;
		
		
		double change_factor = Double.parseDouble(args[2]);
		
		
		List<Id<Person>> poolIds = new ArrayList<Id<Person>>();
		
		HashMap<Id<Person>, List<List<Integer>>> mutableTours = new HashMap<Id<Person>, List<List<Integer>>>();
		
		// count all the tours with mode car from home to home without outside in between
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			// find all home - home tours with car
			List<Integer> homeCarStart = new ArrayList<Integer>();
			List<Integer> homeCarEnd = new ArrayList<Integer>();
			for (int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {
				PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
				if (pe instanceof Activity) {
					// find first home activity that is followed by car trip
					if (homeCarStart.size() == 0) {
						if (((Activity) pe).getType() == "home") {
							if (i+1 < person.getSelectedPlan().getPlanElements().size()) {
								PlanElement nextPe = person.getSelectedPlan().getPlanElements().get(i+1);
								if (nextPe instanceof Leg) {
									if (((Leg) nextPe).getMode() == "car") {
										homeCarStart.add(i);
									}
								}
							}
						}
					}
					// find following home activities 
					else {
						// end of tour
						if (((Activity) pe).getType() == "home") {
							PlanElement beforePe = person.getSelectedPlan().getPlanElements().get(i-1);
							if (beforePe instanceof Leg) {
								if ((((Leg) beforePe).getMode() == "car") || (((Leg) beforePe).getMode() == "car passenger")) {
									homeCarEnd.add(i);
								}	
							}
							// if there is a next leg, also start of new tour
							if (i+1 < person.getSelectedPlan().getPlanElements().size()) {
								PlanElement nextPe = person.getSelectedPlan().getPlanElements().get(i+1);
								if (nextPe instanceof Leg) {
									if (((Leg) nextPe).getMode() == "car") {
										homeCarStart.add(i);
									}
								}
							}
							
						}
					}
					
				}
			}
			// remove inconsistent home tours
			// e.g. home - car - leisure - walk - leisure - car passenger - home
			// -> homeCarStart.size() > homeCarEnd.size()
			// e.g. home - car - work - car - home - walk - leisure - car - home
			// -> homeCarStart.size() < homeCarEnd.size()
			
			while (homeCarStart.size() != homeCarEnd.size()) {
				if (homeCarStart.size() > homeCarEnd.size())
					homeCarStart.remove(homeCarStart.size()-1);
				if (homeCarStart.size() < homeCarEnd.size())
					homeCarEnd.remove(homeCarEnd.size()-1);
				
			}
			
			// System.out.println(person.getId());
			// System.out.println(homeCarStart);
			// System.out.println(homeCarEnd);
			// System.out.println("---");
			
			// check that in between home and home there is no outside
			if ((homeCarStart.size() > 0)) {
				List<Integer> remove = new ArrayList<Integer>();
				for (int j = 0; j < homeCarStart.size(); j++) {
					for (int k = homeCarStart.get(j)+1; k < homeCarEnd.get(j); k++) {
						PlanElement betweenPe = person.getSelectedPlan().getPlanElements().get(k);
						if (betweenPe instanceof Activity) {
							if (((Activity) betweenPe).getType() == "outside") {
								remove.add(j);
								break;
							}	
						}
					}
				}
				Collections.reverse(remove);
				for (int index : remove) {
					homeCarStart.remove(index);
					homeCarEnd.remove(index);
					
				}
			}
			
			// System.out.println(homeCarStart);
			// System.out.println(homeCarEnd);
			// System.out.println("------");
			
			
			if ((homeCarStart.size() > 0)) {
				poolIds.add(person.getId());
				List<List<Integer>> startEndLists = new ArrayList<List<Integer>>();
				startEndLists.add(homeCarStart);
				startEndLists.add(homeCarEnd);
				mutableTours.put(person.getId(), startEndLists);
				tours_count = tours_count + homeCarStart.size();
			}
		}
		
		// random order of agents
		Collections.shuffle(poolIds);
		
		// count mutated tours
		int mutation_count = 0;
		
		int stop = (int) (change_factor * tours_count);
		
		for (Id<Person> id : poolIds) {
			Person person = scenario.getPopulation().getPersons().get(id);	
			for (int j = 0; j < mutableTours.get(id).get(0).size(); j++) {
				for (int k = mutableTours.get(id).get(0).get(j)+1; k < mutableTours.get(id).get(1).get(j); k++) {
					PlanElement betweenPe = person.getSelectedPlan().getPlanElements().get(k);
					if (betweenPe instanceof Leg) {
						if (((Leg) betweenPe).getMode() == "car") {
							((Leg) betweenPe).setMode("pt");
						}
					}
				}
				mutation_count += 1;
			}
			
			if (mutation_count >= stop) {
				break;
				}
		}
		
		System.out.println("All tours ----------------");
		System.out.println(tours_count);
		// Thread.sleep(1500);
		System.out.println("Stop ----------------");
		System.out.println(stop);
		// Thread.sleep(1500);
		System.out.println("Changed tours ----------------");
		System.out.println(mutation_count);
		// Thread.sleep(1500);
		System.out.println("Control ----------------");
		double control = (float) mutation_count / (float) tours_count;
		System.out.println(control);
		// Thread.sleep(1500);
		
		new PopulationWriter(scenario.getPopulation()).write(args[1]);
		
	}
}
