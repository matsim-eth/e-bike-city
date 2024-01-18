package ebikecity.utils;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class VehEnterTimesFromEvents {
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		DepEnterTimesHandler evHandler = new DepEnterTimesHandler();
		
		eventsManager.addHandler(evHandler);
		
		new MatsimEventsReader(eventsManager).readFile(args[0]);
		
		evHandler.writeToCSV(args[1]);
	}

}

