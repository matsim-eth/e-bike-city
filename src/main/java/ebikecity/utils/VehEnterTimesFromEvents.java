package ebikecity.utils;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

// run this to generate a csv file that contains the time of
// PersonDeparture and VehicleEntersTraffic for every car trip

//args
//[0]	path of events file (xml or xml.gz)
//[1]	path of output file (csv)

public class VehEnterTimesFromEvents {
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		DepEnterTimesHandler evHandler = new DepEnterTimesHandler();
		
		eventsManager.addHandler(evHandler);
		
		new MatsimEventsReader(eventsManager).readFile(args[0]);
		
		evHandler.writeToCSV(args[1]);
	}

}

