package ebikecity.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;

// belongs to CountsFromEvents
// stores bike counts on specific links for each hour of the simulation in a matrix while parsing events file
// (City of Zurich recommends to aggregate their counts as well by hour)

public class CountEventHandler implements LinkEnterEventHandler {
	
	private List<String> linksOfInterest = new ArrayList<String>();
	public int[][] countsBike;
	
	public CountEventHandler(List<String> links) {
		this.linksOfInterest = links;
		this.countsBike = new int[this.linksOfInterest.size()][30];
		
	}
	
	// count linkEnterEvents of vehicles xxxxx_bike and assign to hourly bin
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.linksOfInterest.contains(event.getLinkId().toString())) {
			if (event.getVehicleId().toString().contains("bike")) {
				int hour = (int) (Math.floor(event.getTime() / 3600));
				countsBike[linksOfInterest.indexOf(event.getLinkId().toString())][hour]++;				
			}
		}
		
	}
	
	
	// write csv with links as headers and hours as index
	public void writeCounts(String filename) {
		try {
			FileWriter writer = new FileWriter(filename);
			BufferedWriter bwr = new BufferedWriter(writer);
			bwr.write("hour;");
			for (int i = 0; i < this.linksOfInterest.size(); i++) {
				bwr.write(this.linksOfInterest.get(i) + ";");
			}
			bwr.write("\n");
			for (int i = 0; i < 30; i++) {
				bwr.write(String.valueOf(i) + ";");
				for (int j = 0; j < this.linksOfInterest.size(); j++) {
					bwr.write(this.countsBike[j][i] + ";");
				}
				bwr.write("\n");
			}
			bwr.close();	
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}


}
