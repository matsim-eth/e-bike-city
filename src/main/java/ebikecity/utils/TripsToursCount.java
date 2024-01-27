package ebikecity.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;

import ebikecity.project.config.AstraConfigurator;

// run this to create a csv from the plans that gets all tours (start or end at home or outside)
// and all trip ids of tour and the sum of the euclidean distance of the trips of the tour

// args
// [0] config that contains population and network and output directory


public class TripsToursCount {


	public static void main(String[] args) throws IOException, InterruptedException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes( "mode-parameter", "cost-parameter") //
				.build();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), AstraConfigurator.getConfigGroups());
		
		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		ScenarioUtils.loadScenario(scenario);
		
		TripListConverter tripListConverter = new TripListConverter();
		
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		
		ActivityTourFinderConfigGroup tfConfig = dmcConfig.getActivityTourFinderConfigGroup();
		TourFinder tourFinder = new ActivityTourFinder(tfConfig.getActivityTypes());
		
		String outputDirectory = config.controler().getOutputDirectory();
		
		File file = new File(outputDirectory+"/tour_sizes.csv");
		
		String[] headers = {"person", "tour", "n_trips", "mode", "origin", "destination", "sum_eucl_dist", "trip_ids"};
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write headers
            for (int i = 0; i < headers.length; i++) {
                writer.write(headers[i]);
                if (i < headers.length - 1) {
                    writer.write(";");
                }
            }
            writer.newLine();
            
            for (Person person : scenario.getPopulation().getPersons().values()) {
            	int next_trip = 1;
    			List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());
    			List<List<DiscreteModeChoiceTrip>> tours = tourFinder.findTours(trips);
    			for (int i = 0; i < tours.size(); i++) {
    				List<DiscreteModeChoiceTrip> tour = tours.get(i);
    				DiscreteModeChoiceTrip initialTrip = tour.get(0);
    				DiscreteModeChoiceTrip finalTrip = tour.get(tour.size()-1);
    				Double tourLength = 0.0;
    				String trip_ids = "[";
    				for (DiscreteModeChoiceTrip trip : tour) {
    					tourLength = tourLength+
    							CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
    									trip.getDestinationActivity().getCoord());
    					trip_ids = trip_ids + person.getId().toString() + "_" + Integer.toString(next_trip) + ", ";
    					next_trip++;
    				}
    				trip_ids = trip_ids.substring(0, trip_ids.length() - 2) + "]";
    					
    				
    				// Write rows
    			    writer.write(person.getId().toString()); 
    			    writer.write(";");
    			    writer.write(Integer.toString(i));
    			    writer.write(";");
    			    writer.write(Integer.toString(tour.size()));
    			    writer.write(";");
    			    writer.write(initialTrip.getInitialMode().toString());
    			    writer.write(";");
    			    writer.write(initialTrip.getOriginActivity().getType());
    			    writer.write(";");
    			    writer.write(finalTrip.getDestinationActivity().getType());
    			    writer.write(";");
    			    writer.write(Double.toString(tourLength));
    			    writer.write(";");
    			    writer.write(trip_ids);
    			    writer.newLine();
    			}
    				
    		}
    	}
		catch (IOException e) {
            e.printStackTrace();
        }
		
		
	}
}

