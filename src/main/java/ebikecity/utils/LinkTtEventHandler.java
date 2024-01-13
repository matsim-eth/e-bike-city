package ebikecity.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;

public class LinkTtEventHandler implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler,
										   LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
	
	private VehicleLinkParser parser;
	
	class VehicleLinkInfo {
	    private double enterTime;
	    private double leaveTime;

	    public VehicleLinkInfo(double enterTime, double leaveTime) {
	        this.enterTime = enterTime;
	        this.leaveTime = leaveTime;
	    }

	    public double getEnterTime() {
	        return enterTime;
	    }

	    public double getLeaveTime() {
	        return leaveTime;
	    }
	}
	
	public class VehicleLinkParser {
	    private Map<String, Map<String, VehicleLinkInfo>> vehicleLinkData;

	    public VehicleLinkParser() {
	        vehicleLinkData = new HashMap<>();
	    }

	    public void addEnterTime(String vehicleId, String linkId, double enterTime) {
	        vehicleLinkData.putIfAbsent(vehicleId, new HashMap<>());
	        Map<String, VehicleLinkInfo> linkMap = vehicleLinkData.get(vehicleId);

	        // Only add enter time; leave time will be set later
	        linkMap.put(linkId, new VehicleLinkInfo(enterTime, -1));
	    }

	    public void addLeaveTime(String vehicleId, String linkId, double leaveTime) {
	        Map<String, VehicleLinkInfo> linkMap = vehicleLinkData.get(vehicleId);

	        if (linkMap != null) {
	            // Update leave time for the specified link
	            VehicleLinkInfo info = linkMap.get(linkId);
	            if (info != null) {
	                info = new VehicleLinkInfo(info.getEnterTime(), leaveTime);
	                linkMap.put(linkId, info);
	            }
	        }
	    }

	    public VehicleLinkInfo getVehicleLinkInfo(String vehicleId, String linkId) {
	        Map<String, VehicleLinkInfo> linkMap = vehicleLinkData.get(vehicleId);

	        if (linkMap != null) {
	            return linkMap.get(linkId);
	        } else {
	            return null;
	        }
	    }
	}
	
	
	public LinkTtEventHandler() {
		this.parser = new VehicleLinkParser();
			
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		boolean vehicleIsCar = true;
		if (event.getVehicleId().toString().contains("_")) { // we have multiple modes and want only the cars
			if (!event.getVehicleId().toString().contains("car")) {
				vehicleIsCar = false;
			}
		}
		if (vehicleIsCar) {
			this.parser.addEnterTime(event.getVehicleId().toString(), event.getLinkId().toString(),
										   event.getTime());
		}
		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		boolean vehicleIsCar = true;
		if (event.getVehicleId().toString().contains("_")) { // we have multiple modes and want only the cars
			if (!event.getVehicleId().toString().contains("car")) {
				vehicleIsCar = false;
			}
		}
		if (vehicleIsCar) {
			this.parser.addEnterTime(event.getVehicleId().toString(), event.getLinkId().toString(),
										   event.getTime());
		}
		
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		boolean vehicleIsCar = true;
		if (event.getVehicleId().toString().contains("_")) { // we have multiple modes and want only the cars
			if (!event.getVehicleId().toString().contains("car")) {
				vehicleIsCar = false;
			}
		}
		if (vehicleIsCar) {
			this.parser.addLeaveTime(event.getVehicleId().toString(), event.getLinkId().toString(),
										   event.getTime());
		}
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		boolean vehicleIsCar = true;
		if (event.getVehicleId().toString().contains("_")) { // we have multiple modes and want only the cars
			if (!event.getVehicleId().toString().contains("car")) {
				vehicleIsCar = false;
			}
		}
		if (vehicleIsCar) {
			this.parser.addLeaveTime(event.getVehicleId().toString(), event.getLinkId().toString(),
										   event.getTime());
		}
		
	}

	
	public void writeTravelTimes(String filename) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
			
			// Write CSV file headers
            writer.write("vehicle,link,enter_time,leave_time,travel_time");
            writer.newLine();
			
			// Write data to CSV
            for (Map.Entry<String, Map<String, VehicleLinkInfo>> entry : this.parser.vehicleLinkData.entrySet()) {
                String vehicleId = entry.getKey();
                Map<String, VehicleLinkInfo> linkMap = entry.getValue();

                for (Map.Entry<String, VehicleLinkInfo> linkEntry : linkMap.entrySet()) {
                    String linkId = linkEntry.getKey();
                    VehicleLinkInfo info = linkEntry.getValue();
                    int travelTime = (int) (info.getLeaveTime() - info.getEnterTime());

                    // Write a CSV record
                    writer.write(String.format("%s,%s,%d,%d,%d", vehicleId, linkId, (int) info.getEnterTime(), (int) info.getLeaveTime(), travelTime));
                    writer.newLine();
                }
            }	
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}


}


