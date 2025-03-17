package eventHandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
/**
 * 
 * @author Lucas Meyer de Freitas, EBP
 * Calculates the loads of (SBB) trains from the events file and transit schedule. 
 * The selection of for which lines loads are to be calculated can be made by changing the filter in line 35.
 */
public class CalculateTrainLoads {
	
	public List<StopDepLoad> loadsForTransitRoutes(TransitSchedule schedule, String pathToEventsFile) {
		
		MyTransitLoad load = new MyTransitLoad();
	    EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(load);
		new	MatsimEventsReader(manager).readFile(pathToEventsFile);		
	
        List<StopDepLoad> stopLoadList = new ArrayList<StopDepLoad>();
        
	Map<Id<TransitLine>, TransitLine> lines = schedule.getTransitLines();
		for (Map.Entry<Id<TransitLine>, TransitLine> entry : lines.entrySet()) {
			TransitLine line = entry.getValue();
				try {
				if(line.getId().toString().contains("SBB")){
			Map<Id<TransitRoute>, TransitRoute> routes = line.getRoutes(); 
			for (Map.Entry<Id<TransitRoute>, TransitRoute> entry2 : routes.entrySet()) {
				TransitRoute route = entry2.getValue();
				Map<Id<Departure>, Departure> departures = route.getDepartures(); 
					for (Map.Entry<Id<Departure>, Departure> entry3 : departures.entrySet()) {
						Departure departure = entry3.getValue();
						for (int i = 0; i < route.getStops().size(); i++) {
						    int thisLoad = load.getLoadAtDeparture(line, route, i, departure);

							StopDepLoad stopLoad = new StopDepLoad();
							stopLoad.setLine(line.getId().toString());
							stopLoad.setRoute(route.getId().toString());
							stopLoad.setStopIndex(i);
							stopLoad.setLoad(thisLoad);
							
							stopLoadList.add(stopLoad);
				}
			}
		}
			}} catch(NullPointerException e) {
				
			}
			
	}
		
			
		
		return stopLoadList;
	}




	
	
}