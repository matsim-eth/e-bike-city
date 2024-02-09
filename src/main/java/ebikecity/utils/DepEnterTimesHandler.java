package ebikecity.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;

// belongs to VehEnterTimesFromEvents
// for now, only works for simulation where only cars are simulated
// and vehicle id = person id
// (it is a bit tedious, another way would be to store only the personDepartureEvents
// and for each vehicleEntersTrafficEvent get the last recorded personDepartureEvent)

public class DepEnterTimesHandler implements VehicleEntersTrafficEventHandler,
											 PersonDepartureEventHandler {
	
	// helper class to store the event information we want to write to the csv file
	// easier later, because same attributes for both events types (vehicleEntersTraffic and PersonDeparture)
	class MinimalEvent {
	    String type;
	    String person;
	    String link;
	    Double time;
	    String mode;

	    public MinimalEvent(String type, String person, String link, Double time, String mode) {
	        this.type = type;
	        this.person = person;
	        this.link = link;
	        this.time = time;
	        this.mode = mode;
	    }
	}
	
	// for storing events and matching them later when writing csv
	private List<MinimalEvent> personDepartureEvents;
	private Map<String, List<MinimalEvent>> vehicleEntersTrafficMap;
    
    public DepEnterTimesHandler() {
    	personDepartureEvents = new ArrayList<>();
        vehicleEntersTrafficMap = new HashMap<>();
    }

    // store personDepartureEvents of agents starting a car trip
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("car")) {
			personDepartureEvents.add(new MinimalEvent("personDeparture",
				event.getPersonId().toString(), event.getLinkId().toString(), event.getTime(), event.getLegMode()));
		}
	}

	// store vehicleEntersTrafficEvent at beginning of all car trips
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (!event.getPersonId().toString().contains("freight")) {
			vehicleEntersTrafficMap.computeIfAbsent(event.getPersonId().toString(), k -> new ArrayList<>()).add(new MinimalEvent("vehicleEntersTraffic",
				event.getPersonId().toString(), event.getLinkId().toString(), event.getTime(), event.getVehicleId().toString()));
		}
	}
	
	// for every personDepartureEvent find closest vehicleEntersTrafficEvent in the future
	// e.g. if people start multiple trips from their home, they have multiple 
	// personDeparture and vehicleEntersTraffic events on that link
	private MinimalEvent findClosestVehicleEntersTrafficEvent(MinimalEvent personDepartureEvent) {
        List<MinimalEvent> eventsForPerson = vehicleEntersTrafficMap.get(personDepartureEvent.person);
        if (eventsForPerson != null) {
            MinimalEvent closestEvent = null;
            Double closestTimeDiff = Double.MAX_VALUE;

            for (MinimalEvent vehicleEntersTrafficEvent : eventsForPerson) {
                if (personDepartureEvent.link.equals(vehicleEntersTrafficEvent.link)) {
                    Double timeDiff = personDepartureEvent.time - vehicleEntersTrafficEvent.time;

                    if ((timeDiff >= 0.0)&& (timeDiff < closestTimeDiff)) {
                        closestTimeDiff = timeDiff;
                        closestEvent = vehicleEntersTrafficEvent;
                    }
                }
            }

            return closestEvent;
        }

        return null;
    }
	
	// write to csv
	public void writeToCSV(String filename) {
        try (FileWriter csvWriter = new FileWriter(filename)) {
            csvWriter.append("Person,Vehicle,Mode,Link,PersonDepartureTime,VehicleEntersTrafficTime,TimeDiff\n");
            
            for (MinimalEvent personDepartureEvent : personDepartureEvents) {
            	
            	MinimalEvent closestVehicleEntersTrafficEvent = findClosestVehicleEntersTrafficEvent(personDepartureEvent);
            	
            	if (closestVehicleEntersTrafficEvent != null) {
            		csvWriter.append(personDepartureEvent.person)
                        	 .append(",")
                        	 .append(closestVehicleEntersTrafficEvent.mode)
                        	 .append(",")
                        	 .append(personDepartureEvent.mode)
                        	 .append(",")
                        	 .append(personDepartureEvent.link)
                        	 .append(",")
                        	 .append(Double.toString(personDepartureEvent.time))
                        	 .append(",")
                        	 .append(Double.toString(closestVehicleEntersTrafficEvent.time))
                        	 .append(",")
                        	 .append(Double.toString(closestVehicleEntersTrafficEvent.time-personDepartureEvent.time))
                        	 .append("\n");
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	

}
