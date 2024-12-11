package ebikecity.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.text.DecimalFormat;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

// belongs to AnalysisTravelTimeFromEvents

public class AnalysisTravelTimeEventsHandler implements VehicleEntersTrafficEventHandler, LinkEnterEventHandler,
										   LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
	
	private LinkParser parser;
	
	// Helper class to store link enter and leave time of vehicle on one link
	class LinkTimeInfo {
	    private double enterTime;
	    private double leaveTime;
	    private String vehicleId;

	    public LinkTimeInfo(double enterTime, double leaveTime, String vehicleId) {
	        this.enterTime = enterTime;
	        this.leaveTime = leaveTime;
	        this.vehicleId = vehicleId;
	    }

	    public double getEnterTime() {
	        return enterTime;
	    }

	    public double getLeaveTime() {
	        return leaveTime;
	    }
	    
	    public String getVehicleId() {
	        return this.vehicleId;
	    }
	    
	    public String toString() {
	        return "Vehicle " + this.vehicleId + " - " + String.valueOf(this.enterTime) + " - " +  String.valueOf(this.leaveTime);
	    }
	}
	
	// Helper class defining vehicle type + time pairs
	// Example: car_7:00 for all car trips between 7:00 and 7:30
	class VehicleHourInfo {
	    private String vehicleType;
	    private String time;
	    
	    public VehicleHourInfo(String mode, String time){
	        this.vehicleType = mode;
	        this.time = time;
	    }
	    
	    public String toString(){
	    	String label = this.vehicleType + "_" + this.time;
	    	return label;
	    }
	    
	    public boolean equals(VehicleHourInfo info2){
	    	return (this.vehicleType==info2.vehicleType) && (this.time==info2.time);
	    }
	    
	    public String getVehicleType(){
	        return this.vehicleType;
	    }
	    
	    public String getTime(){
	        return this.time;
	    } 
	    
	    public VehicleHourInfo before(){
	    	String suffix =  this.time.substring(3);
	    	String hour = this.time.substring(0, 2);
	    	int hourInt = Integer.parseInt(hour);
	    	int hourBefore = (hourInt - 1);// % 24;
	    	if (0  >= hourBefore){
	    		hourBefore = (24 + hourBefore)%24;
	    	}
	    	String time = "";
	    	if (suffix.equals("30")){
	    		time = hour + ":00";
	    	}
	    	else{
	    		String newHour = String.valueOf(hourBefore);
	    		if (hourBefore  >= 10){
				time = newHour + ":30";				
			}
			else{
				time = "0"+newHour+":30";
			}
	    	}  
	    	VehicleHourInfo beforeInfo = new VehicleHourInfo(this.vehicleType, time);
	    	return beforeInfo;
	    }
		
	}
	
	// Helper class to store link leave and enter events for every link by mode and time bin
	public class LinkParser {
	
	    // As of now, should only work if all vehicles go over each link only once per hour
		
	    // Container mapping LinkId to map of VehicleHour info that contains a map of link time info indexed by vehicle id
	    private Map<Id<Link>, Map<VehicleHourInfo, Map<String, LinkTimeInfo>>> linkData;

	    public LinkParser() {
	        this.linkData = new HashMap<>();
	    }


	    public void addEnterTime(Id<Link> linkId, VehicleHourInfo modeHour, String vehicleId, double enterTime) {
	    	// If this is the first occurence of linkId, then create an entry in the Map
	        linkData.putIfAbsent(linkId, new HashMap<>());
	        
	        // Access the entry in the map
	        Map<VehicleHourInfo, Map<String, LinkTimeInfo>> linkInfo = linkData.get(linkId);
	        
	        // If this is the first occurence of modeHour, create an entry
	        //linkInfo.putIfAbsent(modeHour, new HashMap<>());
	        boolean modeHourAbsent = true;
	        for (Map.Entry<VehicleHourInfo, Map<String, LinkTimeInfo>> entry: linkInfo.entrySet()){
	            VehicleHourInfo label = entry.getKey();
	            if (label.toString().equals(modeHour.toString())){
	                modeHourAbsent = false;
	                Map<String, LinkTimeInfo> linkHourInfo = linkInfo.get(label); 
	                linkHourInfo.put(vehicleId, new LinkTimeInfo(enterTime, -1.0, vehicleId));  
	            }
	        }
	        
	        if (modeHourAbsent){
	            linkInfo.putIfAbsent(modeHour, new HashMap<>());
	            Map<String, LinkTimeInfo> linkHourInfo = linkInfo.get(modeHour); 
	            linkHourInfo.put(vehicleId, new LinkTimeInfo(enterTime, -1.0, vehicleId));  
	        }	   
	    }


	    public void addLeaveTime(Id<Link> linkId, VehicleHourInfo modeHour, String vehicleId, double leaveTime) {
	        // Get entry from linkId
	        Map<VehicleHourInfo, Map<String, LinkTimeInfo>> linkInfo = linkData.get(linkId);	        
	        boolean wasFound=false;
	        if (linkInfo != null) {
	            // Access information for the specific link
	            for (Map.Entry<VehicleHourInfo, Map<String, LinkTimeInfo>> entry: linkInfo.entrySet()){	            	
	            	// Try identifying the entry for the same hour
	                VehicleHourInfo label = entry.getKey();	               
	                // If there is already a record for that hour
	                if (label.toString().equals(modeHour.toString())) {	          
	                    Map<String, LinkTimeInfo> linkHourInfo = entry.getValue();	                    
	                    if (linkHourInfo != null) {	            		
	            		// Try finding an entry corresponding to the vehicle ID
	                	LinkTimeInfo info = linkHourInfo.get(vehicleId);	                
	                	if (info != null) {	                    
	                    		info = new LinkTimeInfo(info.getEnterTime(), leaveTime, vehicleId);
	                    		linkHourInfo.put(vehicleId, info);
	                    		wasFound = true;	                    
	                	}	            
	            	    }
	                }	                
	            }
	           
	            // Else try with the hour before
	            if (!wasFound) {	
	            	VehicleHourInfo beforeTimeInfo = modeHour.before();
	            	while (!wasFound){              
	                	for (Map.Entry<VehicleHourInfo, Map<String, LinkTimeInfo>> entry: linkInfo.entrySet()){
	                		VehicleHourInfo hourinfo = entry.getKey();
	                		Map<String, LinkTimeInfo> mapsVehicleTimes = entry.getValue();
	                		if (hourinfo.toString().equals(beforeTimeInfo.toString())){
	                			LinkTimeInfo info = mapsVehicleTimes.get(vehicleId);
	                			if (info != null){
	                				info = new LinkTimeInfo(info.getEnterTime(), leaveTime, vehicleId);
	                				mapsVehicleTimes.put(vehicleId, info);
	                				wasFound = true;
	                			}
	                		}	                
	                	} // endFor 
	                	beforeTimeInfo = beforeTimeInfo.before();                
	               }
	            } // end If ! wasFound
	        }
	    }

	    public LinkTimeInfo getLinkTimeInfo(Id<Link> linkId, VehicleHourInfo modeHour, String vehicleId) {	    
	        Map<VehicleHourInfo, Map<String, LinkTimeInfo>> linkInfo = linkData.get(linkId);
	        if (linkInfo != null) {	        
	            for (Map.Entry<VehicleHourInfo, Map<String, LinkTimeInfo>> entry: linkInfo.entrySet()){
	            	VehicleHourInfo label = entry.getKey();
	                	if (label.toString().equals(modeHour.toString())) {
	                		Map<String, LinkTimeInfo> linkHourInfo = entry.getValue();
	                		if (linkHourInfo != null) {	            	
	            				LinkTimeInfo info = linkHourInfo.get(vehicleId);	            	
	            				return info;           	
	            			}
	            			else {	            	
	            				return null;
	            			}
	                	}	            
	            }	            
	            return null;
	        }
	        else {	        
	            return null;	            
	        }
	    } // end function
	    
	} // end class
	
	
	public AnalysisTravelTimeEventsHandler() {
		this.parser = new LinkParser();
			
	}
	
	// connect eventHandler and parser functions
	// treat VehicleEntersTrafficEvent same as LinkEnterEvent
	// and VehicleLeavesTrafficEvent as LinkLeaveEvent
	// to not write the same for both delegated to parser
	
	public static String secondsToTimeBin(double eventTime) {
	
		int hour = (int) (Math.floor(eventTime / 3600));
		int min = (int) (Math.floor((eventTime - (3600*hour)) / 60));
		String suffix = ":00";
		if (min >= 30){
			suffix=":30";
		}
		int hourDay = hour ;//% 24;
		String result = String.valueOf(hourDay) + suffix;
		if (hourDay  >= 10){
			return result;
		}
		return "0"+result;	
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {	
		boolean vehicleIsCar = true;
		boolean vehicleIsBike = true;
		
		if (event.getVehicleId().toString().contains("_")){
			if (!event.getVehicleId().toString().contains("car")){
				vehicleIsCar = false;
			}
			if (!event.getVehicleId().toString().contains("bike")){
				vehicleIsBike = false;
			}
		}		
			
		String time = secondsToTimeBin(event.getTime());		
		String mode = "unknown";
				
		if (vehicleIsCar){
		    mode = "car";
		}
			
		if (vehicleIsBike){
		    mode = "bike";
		}
		
		if (vehicleIsCar && vehicleIsBike){
		    mode = "car";
		}
			
		VehicleHourInfo vehicleHourInfo = new VehicleHourInfo(mode, time);		
		if (vehicleIsCar || vehicleIsBike) {
			this.parser.addEnterTime(event.getLinkId(), vehicleHourInfo, event.getVehicleId().toString(), event.getTime());
		}		
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {	
		boolean vehicleIsCar = true;
		boolean vehicleIsBike = true;
		
		if (event.getVehicleId().toString().contains("_")){
			if (!event.getVehicleId().toString().contains("car")){
				vehicleIsCar = false;
			}
			if (!event.getVehicleId().toString().contains("bike")){
				vehicleIsBike = false;
			}
		}		
			
		String time = secondsToTimeBin(event.getTime());		
		String mode = "unknown";
				
		if (vehicleIsCar){
		    mode = "car";
		}
			
		if (vehicleIsBike){
		    mode = "bike";
		}
		
		if (vehicleIsCar && vehicleIsBike){
		    mode = "car";
		}
			
		VehicleHourInfo vehicleHourInfo = new VehicleHourInfo(mode, time);		
		if (vehicleIsCar || vehicleIsBike) {
			this.parser.addEnterTime(event.getLinkId(), vehicleHourInfo, event.getVehicleId().toString(), event.getTime());
		}
		
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		boolean vehicleIsCar = true;
		boolean vehicleIsBike = true;
		
		if (event.getVehicleId().toString().contains("_")){
			if (!event.getVehicleId().toString().contains("car")){
				vehicleIsCar = false;
			}
			if (!event.getVehicleId().toString().contains("bike")){
				vehicleIsBike = false;
			}
		}		
			
		String time = secondsToTimeBin(event.getTime());		
		String mode = "unknown";
				
		if (vehicleIsCar){
		    mode = "car";
		}
			
		if (vehicleIsBike){
		    mode = "bike";
		}
		
		if (vehicleIsCar && vehicleIsBike){
		    mode = "car";
		}
			
		VehicleHourInfo vehicleHourInfo = new VehicleHourInfo(mode, time);		
		if (vehicleIsCar || vehicleIsBike) {
			this.parser.addLeaveTime(event.getLinkId(), vehicleHourInfo, event.getVehicleId().toString(), event.getTime());
		}		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		boolean vehicleIsCar = true;
		boolean vehicleIsBike = true;
		
		if (event.getVehicleId().toString().contains("_")){
			if (!event.getVehicleId().toString().contains("car")){
				vehicleIsCar = false;
			}
			if (!event.getVehicleId().toString().contains("bike")){
				vehicleIsBike = false;
			}
		}		
			
		String time = secondsToTimeBin(event.getTime());		
		String mode = "unknown";
				
		if (vehicleIsCar){
		    mode = "car";
		}
			
		if (vehicleIsBike){
		    mode = "bike";
		}
		
		if (vehicleIsCar && vehicleIsBike){
		    mode = "car";
		}
			
		VehicleHourInfo vehicleHourInfo = new VehicleHourInfo(mode, time);		
		if (vehicleIsCar || vehicleIsBike) {
			this.parser.addLeaveTime(event.getLinkId(), vehicleHourInfo, event.getVehicleId().toString(), event.getTime());
		}				
	}	

	// TODO change to median?
	public double computeAverageTravelTime(Map<String, LinkTimeInfo> travelInfo, double freeflowTT){	
	    int nSamples = travelInfo.size();	
	    double sumOfTravelTimes = 0;	    
	    int cpt = 0;
	    
	    for (Map.Entry<String, LinkTimeInfo> entry: travelInfo.entrySet()){
	        LinkTimeInfo info = entry.getValue();
	        if (info.getLeaveTime()>0){
	        	sumOfTravelTimes += info.getLeaveTime() - info.getEnterTime();
	        	cpt +=1;
	        }
	        else{
	        	return Double.POSITIVE_INFINITY;
	        }
	    }	    
	    
	    //if (sumOfTravelTimes <= cpt*freeflowTT){
	    //	return freeflowTT;
	    //}
	    
	    return sumOfTravelTimes / cpt;	
	}

	// write information to csv
	public void writeTravelTimes(String filename, String networkPath) {
	
		DecimalFormat df = new DecimalFormat("0.00");
	
	        Config config = ConfigUtils.createConfig();
	        Scenario scenario = ScenarioUtils.createMutableScenario(config);
	        MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(networkPath);
		Network network = scenario.getNetwork();	
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		
		double bikeFreeSpeed = 15000.0 / 3600.0; // 15km/h
		
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("epsg:2056", "WGS84");
	
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
			
		// Write CSV file headers
            	writer.write("LinkId,OSM_ID,FreeflowTTCar,FreeFlowTTBike,");
            	VehicleHourInfo[] infoLabels = new VehicleHourInfo[96];
            	
            	for (int i=0; i<24; i++){
            	    writer.write("car_"+String.valueOf(i)+":00"+",");
            	    writer.write("N_cars_" + String.valueOf(i)+":00"+",");
            	    writer.write("bike_"+String.valueOf(i)+":00"+",");
            	    writer.write("N_bikes_" + String.valueOf(i)+":00"+",");
            	    writer.write("car_"+String.valueOf(i)+":30"+",");
            	    writer.write("N_cars_" + String.valueOf(i)+":30"+",");
            	    writer.write("bike_"+String.valueOf(i)+":30"+",");
            	    writer.write("N_bikes_" + String.valueOf(i)+":30"+",");
            	    
            	    String hourToString = String.valueOf(i);
            	    if (i < 10){
            	    	hourToString = "0" + hourToString;
            	    }
            	    
            	    infoLabels[4*i] = new VehicleHourInfo("car", hourToString +":00"); 
            	    infoLabels[4*i+1] = new VehicleHourInfo("bike", hourToString +":00");  
            	    infoLabels[4*i+2] = new VehicleHourInfo("car", hourToString +":30");          	    
            	    infoLabels[4*i+3] = new VehicleHourInfo("bike", hourToString +":30");
            	}
            	writer.newLine();
			
		// Write data to CSV
            	for (Map.Entry<Id<Link>, Map<VehicleHourInfo, Map<String, LinkTimeInfo>>> entry : this.parser.linkData.entrySet()) {
                	Id<Link> linkId = entry.getKey();
                	Map<VehicleHourInfo, Map<String, LinkTimeInfo>> linkMap = entry.getValue();
                	
                	writer.write(linkId.toString() + ",");
                	
                	//Add link information
                	Link link = links.get(linkId);
                	//Coord fromXY = link.getFromNode().getCoord();
                	//Coord toXY = link.getToNode().getCoord();
                	
                	double freeFlowTravelTimeCar = link.getLength() / link.getFreespeed();
                	double freeFlowTravelTimeBike = link.getLength() / bikeFreeSpeed;
                	
                	String osmId = link.getAttributes().getAttribute("osm:way:id").toString();
                	writer.write(osmId.toString() + ",");
                	
                	//Coord fromXYnew = transformation.transform(fromXY);
                	//Coord toXYnew = transformation.transform(toXY);
                	
                	freeFlowTravelTimeCar = Double.parseDouble(df.format(freeFlowTravelTimeCar));
                	freeFlowTravelTimeBike = Double.parseDouble(df.format(freeFlowTravelTimeBike));
                	
                	writer.write(String.valueOf(freeFlowTravelTimeCar) + ",");
                	writer.write(String.valueOf(freeFlowTravelTimeBike) + ",");
                	//writer.write(String.valueOf(fromXYnew.getX()) + ",");    
                	//writer.write(String.valueOf(fromXYnew.getY()) + ",");   
                	//writer.write(String.valueOf(toXYnew.getX()) + ",");   
                	//writer.write(String.valueOf(toXYnew.getY()) + ",");                 	  
                	
                	for (int j=0; j<96; j++) {
                	
                		VehicleHourInfo label = infoLabels[j];
                		boolean isCellWritten = false;
                		 
                		double freeFlowTravelTime = 0;
                		if(j%2==0){
                		    freeFlowTravelTime = freeFlowTravelTimeCar;
                		}
                		else{
                		    freeFlowTravelTime = freeFlowTravelTimeBike;
                		}
                		
                		for (Map.Entry<VehicleHourInfo, Map<String, LinkTimeInfo>> subentry : linkMap.entrySet()){
                		    
                		    VehicleHourInfo info = subentry.getKey();
                		    Map<String, LinkTimeInfo> travelTimes = subentry.getValue();
                		    
                		    if (label.toString().equals(info.toString())){
                		    
                		        // compute the average travel time
                		        double avTravelTime = computeAverageTravelTime(travelTimes, freeFlowTravelTime);
                		        int flow = travelTimes.size();
                		        
                		        if (avTravelTime >= 1){
                		        	double roundedAvTravelTime = avTravelTime;
                		        	if (avTravelTime < 1000000){
                		        		roundedAvTravelTime = Double.parseDouble(df.format(avTravelTime));
                		        	}
                		        	writer.write(String.valueOf(roundedAvTravelTime)+",") ; 
                		        	writer.write(String.valueOf(flow)+",") ; 
                		        	isCellWritten = true;
                		        	break; 
                		        }
                		        else {
                		        	// the negative travel times bug should be fixed
                		        	writer.write(String.valueOf(freeFlowTravelTime)+",") ; 
                		        	writer.write(String.valueOf(flow)+",") ; 
                		        	isCellWritten = true;
                		        	break;
                		        }
                		    }
                		
                		}
                		
                		if (!isCellWritten){
                		    // if no records found, use freespeed
                		    writer.write("NA,");
                		    writer.write("0,");
                		}
                	}
                	
                	writer.newLine();
			
		}
		
		writer.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}


}


