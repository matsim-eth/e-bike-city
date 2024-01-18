package ebikecity.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;


public class EstimatedTtFromOutputPlans {
	

	public static void main(String[] args) throws IOException, InterruptedException {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		
				
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]))){
			
			// Write CSV file headers
            writer.write("person,trip_number,trip_id,mode,travel_time");
            writer.newLine();
			
			// Write data to CSV
            for (Person person : scenario.getPopulation().getPersons().values()) {
            	List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            	for (int i = 0; i < trips.size(); i++) {
            		Trip trip = trips.get(i);
            		List<Leg> legs = trip.getLegsOnly();
            		if (legs.size() == 1) { // bike, car or walk routed without access/egress walk
            			if (legs.get(0).getMode().toString() != "walk") {
            				String personId = person.getId().toString();
            				String tripNumber = Integer.toString(i+1);
            				String mode = legs.get(0).getMode();
        					String travelTime = Double.toString(legs.get(0).getTravelTime().seconds());
        					writer.write(String.format("%s,%s,%s,%s,%s", personId, tripNumber, personId+"_"+tripNumber, mode, travelTime));
        	                writer.newLine();
            			}
            		}
            		if (legs.size() == 3) { // (e-)bike or car with access/egress walk
            			String personId = person.getId().toString();
            			String tripNumber = Integer.toString(i+1);
            			String mode = legs.get(1).getMode();
        				String travelTime = Double.toString(legs.get(1).getTravelTime().seconds());
        				writer.write(String.format("%s,%s,%s,%s,%s", personId, tripNumber, personId+"_"+tripNumber, mode, travelTime));
        	            writer.newLine();
            		}
            	}
    		}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

