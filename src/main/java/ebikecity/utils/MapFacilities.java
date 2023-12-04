package ebikecity.utils;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.GeodeticCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class MapFacilities {
	
		public static double calculateAbsAngle(Link link1, Link link2) throws NoSuchAuthorityCodeException, FactoryException {
			
			// CoordinateReferenceSystem crs = CRS.decode("EPSG:2056");
			
			CoordinateReferenceSystem crs = MGC.getCRS("WGS84");
			GeodeticCalculator calc = new GeodeticCalculator(crs);
			
			
			CoordinateTransformation tf = TransformationFactory.getCoordinateTransformation("CH1903_LV03_Plus", "WGS84");
			Coord fromNode1 = tf.transform(link1.getFromNode().getCoord());
			Coord toNode1 = tf.transform(link1.getToNode().getCoord());
			
			Coord fromNode2 = tf.transform(link2.getFromNode().getCoord());
			Coord toNode2 = tf.transform(link2.getToNode().getCoord());
				
			calc.setStartingGeographicPoint(fromNode1.getX(), fromNode1.getY());
			calc.setDestinationGeographicPoint(toNode1.getX(), toNode1.getY());
			
			double azimuth1 = calc.getAzimuth();
		
			calc.setStartingGeographicPoint(fromNode2.getX(), fromNode2.getY());
			calc.setDestinationGeographicPoint(toNode2.getX(), toNode2.getY());
			
			double azimuth2 = calc.getAzimuth();
		
			double angle = Math.abs(azimuth2 - azimuth1);
		
			return angle;
		}

	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {
		
		// scenario for classic input
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);
				
		MatsimNetworkReader netReaderClassic = new MatsimNetworkReader(scenario.getNetwork());
		netReaderClassic.readFile(args[0]);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[1]);

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(args[2]);

		// scenario for new network
		Config configNew = ConfigUtils.createConfig();

		Scenario scenarioNew = ScenarioUtils.createMutableScenario(configNew);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenarioNew.getNetwork());
		netReader.readFile(args[3]);
		
						
		// set up link filter for new network
				
		// all car links
		NetworkFilterManager n = new NetworkFilterManager(scenarioNew.getNetwork());
		n.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAllowedModes().contains("car");
			}
		});
				
		// all motorway links
		NetworkFilterManager m = new NetworkFilterManager(scenarioNew.getNetwork());
		m.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().equals("motorway"); // only motorway
			}
					
		});
		
		// all motorway link links
				NetworkFilterManager ml = new NetworkFilterManager(scenarioNew.getNetwork());
				ml.addLinkFilter(new NetworkLinkFilter() {
							
					@Override
					public boolean judgeLink(Link l) {
						return l.getAttributes().getAttribute("osm:way:highway").toString().equals("motorway_link"); // only motorway_link
					}
							
				});
				
		// all trunk links
		NetworkFilterManager t = new NetworkFilterManager(scenarioNew.getNetwork());
		t.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().equals("trunk"); // only trunk
				}
					
		});
		
		// all trunk link links
				NetworkFilterManager tl = new NetworkFilterManager(scenarioNew.getNetwork());
				tl.addLinkFilter(new NetworkLinkFilter() {
							
					@Override
					public boolean judgeLink(Link l) {
						return l.getAttributes().getAttribute("osm:way:highway").toString().equals("trunk_link"); // only trunk_link
						}
							
				});
				
		// all primary links
		NetworkFilterManager p = new NetworkFilterManager(scenarioNew.getNetwork());
		p.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAttributes().getAttribute("osm:way:highway").toString().contains("primary"); // cover primary and primary_link
			}
					
		});
				
				
		// filtered subnetworks
			
		Network nn = n.applyFilters();
				
		Network nm = m.applyFilters();
		
		Network nml = ml.applyFilters();
			
		Network nt = t.applyFilters();
		
		Network ntl = tl.applyFilters();
		
		// if there are no trunk links in the new network (but maybe in the network cut by eqasim), 
		// use motorway subnetwork instead of trunk subnetwork (NEB Strecken)
		if (nt.getLinks().size() == 0) {
			nt = nm;
			ntl = nml;
			t = m;
			tl = ml;
		}		
				
			
		// map the facilities
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
			Link originalLink = scenario.getNetwork().getLinks().get(facility.getLinkId());
			
			String roadType = originalLink.getAttributes().getAttribute("osm:way:highway").toString();
			
			if (roadType.contains("motorway") || roadType.contains("trunk") || roadType.contains("primary")) {
				
				Network nmtp;
				
				if (roadType.equals("motorway")) {
					nmtp = m.applyFilters();
				}
				else if (roadType.equals("motorway_link")) {
					nmtp = ml.applyFilters();
				}
				
				else if (roadType.equals("trunk")) {
					nmtp = t.applyFilters();
				}
				else if (roadType.equals("trunk_link")) {
					nmtp = t.applyFilters();
				}
				else {
					nmtp = p.applyFilters();
				}
				
				
				Map<Id<Link>, Double> nearLinks = new HashMap<>();
				
				for (int i = 0; i < 5; i++) {
					
					Link nearestLink = NetworkUtils.getNearestLinkExactly(nmtp, facility.getCoord());
					
					// System.out.println("motorway" + nearestLink.getId().toString());
					
					nearLinks.put(nearestLink.getId(), calculateAbsAngle(nearestLink, originalLink));
					
					nmtp.removeLink(nearestLink.getId());
				}
				
				Id<Link> selectedLink = Collections.min(nearLinks.entrySet(), Map.Entry.comparingByValue()).getKey();
				
				((ActivityFacilityImpl) facility).setLinkId(selectedLink);
								
			}
			
			else {
				
				Link nearestLink;
				
				nearestLink = NetworkUtils.getNearestLinkExactly(nn, facility.getCoord());
											
				Node fromNode = nearestLink.getFromNode();
			
				Node toNode = nearestLink.getToNode();
			
				// Map<Id<Link>, Link> map = (Map<Id<Link>, Link>) fromNode.getInLinks().values();
				Collection<Link> inLinks = (Collection<Link>) fromNode.getInLinks().values();
				
							
				Link oppositeLink = null;
				
				for (Link link : inLinks) {
					if (link.getFromNode().getId().toString().equals(toNode.getId().toString())) {
						oppositeLink = link;
					}
					// if there is no opposite link, assume that nearest link is best choice, regardless of direction
					else {
						oppositeLink = nearestLink;
					}
				}
				
				if (calculateAbsAngle(nearestLink, originalLink) > 0.5 * Math.PI) {
							((ActivityFacilityImpl) facility).setLinkId(oppositeLink.getId());
				}
				else {
						((ActivityFacilityImpl) facility).setLinkId(nearestLink.getId());
				}
			
			}					
			
		}
		
		// write facilities to file
		
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[5]);
		
		MatsimFacilitiesReader facReaderNew = new MatsimFacilitiesReader(scenarioNew);
		facReaderNew.readFile(args[5]);
		
				
		// map the activities in the population
		
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan removePlan = null;
			for (Plan plan : person.getPlans()) {
				if (!plan.equals(person.getSelectedPlan()))
					removePlan = plan;
			}
			if (removePlan != null) {
				person.getPlans().remove(removePlan);
			}
			
			
			// set link id of activities to link id from now mapped facility
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().toString() != "pt interaction") {
													
						Id<Link> linkID = scenarioNew.getActivityFacilities().getFacilities().get(((Activity) pe).getFacilityId()).getLinkId();
						((Activity) pe).setLinkId(linkID);
					}
					
				}
			}
			
			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
			for (Trip trip : trips) {
				for (Leg leg : trip.getLegsOnly()) {
					leg.setRoute(null);

				}
			}		
		}
		
		// write mapped population
		
		new PopulationWriter(scenario.getPopulation()).write(args[4]);
			
	}
	
}
