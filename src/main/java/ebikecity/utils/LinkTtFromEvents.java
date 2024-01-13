package ebikecity.utils;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class LinkTtFromEvents {
	
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		LinkTtEventHandler evHandler = new LinkTtEventHandler();
		
		eventsManager.addHandler(evHandler);
		
		new MatsimEventsReader(eventsManager).readFile(args[0]);
		
		evHandler.writeTravelTimes(args[1]);
		
		
		
	
	}
	
	
}
