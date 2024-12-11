package ebikecity.utils;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

//run this to generate a csv file of enter and leave times recorded for every vehicle and link

//args
//[0]	path of events file (xml or xml.gz)
//[1]	path of output file (csv)

public class LinkTtFromEvents {
	
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		LinkTtEventHandler evHandler = new LinkTtEventHandler();
		
		eventsManager.addHandler(evHandler);
		
		new MatsimEventsReader(eventsManager).readFile(args[0]);
		
		evHandler.writeTravelTimes(args[1]);
	
	}
	
	
}
