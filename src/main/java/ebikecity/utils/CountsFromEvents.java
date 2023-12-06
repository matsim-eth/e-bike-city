package ebikecity.utils;

import java.util.Arrays;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class CountsFromEvents {
	
	public static void main(String[] args) {
		
		List<String> links = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		CountEventHandler evHandler = new CountEventHandler(links);
		
		eventsManager.addHandler(evHandler);
		
		new MatsimEventsReader(eventsManager).readFile(args[0]);
		
		evHandler.writeCounts(args[1]);
		
		
		
	
	}
	
	
}
