package ebikecity.project.mode_choice;

import java.util.ArrayList;
import java.util.Collections;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

public class EBikeControlerListener implements BeforeMobsimListener {
	
	private Scenario scenario;

	@Inject
	public EBikeControlerListener(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		if (event.getIteration() == 0) {
			
			Vehicles allVehicles = VehicleUtils.getOrCreateAllvehicles( scenario );
			
			// Vehicles map = (Vehicles) scenario.getScenarioElement( "allvehicles" );
			
			
			// count bikes and set networkMode to bike
			
			int bike_count = 0;
			
			for (Vehicle v : allVehicles.getVehicles().values()) {
				if (v.getType().toString() == "bike") {
					v.getAttributes().putAttribute("netwokMode", "bike");
					bike_count++;
					
				}
			
			}
			
			// convert 20% of bikes to ebike
			
			int ebike_count = 0;
			
			// randomize order of vehicles
			
			ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>(allVehicles.getVehicles().values());
			
			Collections.shuffle(vehicles);
			
			
			while (ebike_count <= (int) (0.2 * bike_count)) {
				for (Vehicle v : vehicles) {
					if (v.getType().toString() == "bike") {
						v.getAttributes().putAttribute("maximumVelocity", 25.0/3.6);
						ebike_count++;
						
					}
				}
				
			}
			 
			
		}
		
	}

}
