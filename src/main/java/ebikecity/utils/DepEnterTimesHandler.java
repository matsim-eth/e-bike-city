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

public class DepEnterTimesHandler implements VehicleEntersTrafficEventHandler,
											 PersonDepartureEventHandler {
	
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
	
	private List<MinimalEvent> personDepartureEvents;
	private Map<String, List<MinimalEvent>> vehicleEntersTrafficMap;
    
    public DepEnterTimesHandler() {
    	personDepartureEvents = new ArrayList<>();
        vehicleEntersTrafficMap = new HashMap<>();
    }

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("car")) {
			personDepartureEvents.add(new MinimalEvent("personDeparture",
				event.getPersonId().toString(), event.getLinkId().toString(), event.getTime(), event.getLegMode()));
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (!event.getPersonId().toString().contains("freight")) {
			vehicleEntersTrafficMap.computeIfAbsent(event.getPersonId().toString(), k -> new ArrayList<>()).add(new MinimalEvent("vehicleEntersTraffic",
				event.getPersonId().toString(), event.getLinkId().toString(), event.getTime(), event.getVehicleId().toString()));
		}
	}
	
	private MinimalEvent findClosestVehicleEntersTrafficEvent(MinimalEvent personDepartureEvent) {
        List<MinimalEvent> eventsForPerson = vehicleEntersTrafficMap.get(personDepartureEvent.person);
        if (eventsForPerson != null) {
            MinimalEvent closestEvent = null;
            Double closestTimeDiff = Double.MAX_VALUE;

            for (MinimalEvent vehicleEntersTrafficEvent : eventsForPerson) {
                if (personDepartureEvent.link.equals(vehicleEntersTrafficEvent.link)) {
                    Double timeDiff = Math.abs(personDepartureEvent.time - vehicleEntersTrafficEvent.time);

                    if (timeDiff < closestTimeDiff) {
                        closestTimeDiff = timeDiff;
                        closestEvent = vehicleEntersTrafficEvent;
                    }
                }
            }

            return closestEvent;
        }

        return null;
    }
	
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
