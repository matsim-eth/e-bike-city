package ebikecity.project.mode_choice.estimators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;

public class UtilityControlerListener implements BeforeMobsimListener {
	
	private final String outputDirectory;
	
	@Inject
	public UtilityControlerListener(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@Override
	// write after each iteration and empty out utility container
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		if (event.getIteration() == 1) {
			
			File file = new File(outputDirectory+"/output_utilities.csv");
			
			String[] headers = {"person", "trip_id", "dep_time", "origin_fac", "mode", "trav_time", "utility", "iteration"};
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
	            // Write headers
	            for (int i = 0; i < headers.length; i++) {
	                writer.write(headers[i]);
	                if (i < headers.length - 1) {
	                    writer.write(",");
	                }
	            }
	            writer.newLine();

	    	}
			    
	        catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		
		if (event.getIteration() >= 1) {
			
			File file = new File(outputDirectory+"/output_utilities.csv");
			
			
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
				
				// Write rows
	            UtilityContainer container = UtilityContainer.getInstance();
	    		for (List<String> tripList : container.getUtilites()) {
	    			for (int i = 0; i < tripList.size(); i++) {
	                    writer.write(tripList.get(i));
	                    if (i < tripList.size()) {
	                        writer.write(",");
	                    }
	                }
	    			writer.write(Integer.toString(event.getIteration()));
	                writer.newLine();	
	    		}
	    		container.getUtilites().clear();

	    	}
			    
	        catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		
		
	}

}


