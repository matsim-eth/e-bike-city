package ebikecity.utils;

import java.util.Arrays;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

// run this to generate a csv file with hourly bike counts for selected links

// args
// [0]	path of events file (xml or xml.gz)
// [1]	path of output file (csv)
// [2] ... [n] links of interest 

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
