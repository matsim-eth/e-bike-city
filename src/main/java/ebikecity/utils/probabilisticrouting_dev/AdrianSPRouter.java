package ebikecity.utils.probabilisticrouting_dev;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.facilities.Facility;

import com.opencsv.CSVReader;

public class AdrianSPRouter {

	private final Network network;
	private final NetworkRoutingModule router;
	private ArrayList<Double> comptimes;

	public AdrianSPRouter(Network network, String configPath, String eventsFilename) {
		this.network = network;
		this.comptimes = new ArrayList<Double>();

		Config config = ConfigUtils.loadConfig(configPath);

//		StochasticDijkstraFactory factory = new StochasticDijkstraFactory();
		SpeedyALTFactory factory = new SpeedyALTFactory();

		// AStarLandmarksFactory
		// SpeedyDijkstraFactory
		// DijkstraFactory
		// StochasticDijkstraFactory

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.configure(config.travelTimeCalculator());
		TravelTimeCalculator ttc = builder.build();

		TravelTime tt = ttc.getLinkTravelTimes();

		TravelDisutility td = new OnlyTimeDependentTravelDisutility(tt);

		LeastCostPathCalculator routeAlgo = factory.createPathCalculator(network, td, tt);
		this.router = new NetworkRoutingModule(TransportMode.bike, PopulationUtils.getFactory(), network, routeAlgo);
	}

	public void run(String relationsCsvPath, String outputPath) {

		try {

			ArrayList<Id<Link>> linkList = new ArrayList<>();
			for (Link link : network.getLinks().values()) {
				if (link.getAllowedModes().contains(TransportMode.bike)) {
					Id<Link> linkId = link.getId();
					linkList.add(linkId);
				}
			}

			CSVReader reader = new CSVReader(new FileReader(relationsCsvPath), ',');
			reader.readNext();
			int linesRead = 0;
//			ArrayList<Double> compTimes = new ArrayList<>();
			ArrayList<Double> distances = new ArrayList<>();
			ArrayList<Integer> linkCounts = new ArrayList<>();
			ArrayList<ArrayList<Id>> routes = new ArrayList<>();
			ArrayList<Double> SPoverlaps = new ArrayList<>();
			ArrayList<Double> overlaps = new ArrayList<>();
			ArrayList<Integer> uniqueRoutes = new ArrayList<>();
			ArrayList<Double> accuracies = new ArrayList<>();
			
	        ArrayList originNodes = readMappedOds().get(0);
	        ArrayList destinationNodes = readMappedOds().get(1);
			
//            ArrayList originNodes = readValidationOds().get(0);
//            ArrayList destinationNodes = readValidationOds().get(1);
//            ArrayList observedRoutes = readValidationOds().get(2);
            
			String[] arr;
			while ((arr = reader.readNext()) != null && linesRead < 1000) {
				if (arr.length > 0) {
					
					// only read lines that have stuff, and ignore empty lines
					String id = arr[0];
					int j = linesRead;
					
					// read from inputfile
//					float fromX = Float.parseFloat(arr[10]);
//					float fromY = Float.parseFloat(arr[9]);
//					float toX = Float.parseFloat(arr[12]);
//					float toY = Float.parseFloat(arr[11]);
					
					
					// random from network
					int randomLinkId1 = (int) (Math.random() * linkList.size());
					int randomLinkId2 = (int) (Math.random() * linkList.size());
					float fromX = (float) network.getLinks().get(linkList.get(randomLinkId1)).getFromNode().getCoord()
							.getX();
					float fromY = (float) network.getLinks().get(linkList.get(randomLinkId1)).getFromNode().getCoord()
							.getY();
					float toX = (float) network.getLinks().get(linkList.get(randomLinkId2)).getFromNode().getCoord()
							.getX();
					float toY = (float) network.getLinks().get(linkList.get(randomLinkId2)).getFromNode().getCoord()
							.getY();
					
					// specific node ID
//	                Id<Node> originNodeId = Id.createNodeId("1644");
//	                Id<Node> destinationNodeId = Id.createNodeId("17377");
//					float fromX = (float) network.getNodes().get(originNodeId).getCoord().getX();
//					float fromY = (float) network.getNodes().get(originNodeId).getCoord().getY();
//					float toX = (float) network.getNodes().get(destinationNodeId).getCoord().getX();
//					float toY = (float) network.getNodes().get(destinationNodeId).getCoord().getY();
					
					// read from validation set
//	                Id<Node> originNodeId = Id.createNodeId(originNodes.get(j).toString());
//	                Id<Node> destinationNodeId = Id.createNodeId(destinationNodes.get(j).toString());
//					float fromX = (float) network.getNodes().get(originNodeId).getCoord().getX();
//					float fromY = (float) network.getNodes().get(originNodeId).getCoord().getY();
//					float toX = (float) network.getNodes().get(destinationNodeId).getCoord().getX();
//					float toY = (float) network.getNodes().get(destinationNodeId).getCoord().getY();
	                
					ArrayList<Double> SPoverlapsODwise = new ArrayList<>();
					ArrayList<Double> accuracyODwise = new ArrayList<>();
					ArrayList<ArrayList<Id>> routesODwise = new ArrayList<>();
					
					for (int predictPerOd = 1; predictPerOd < 2; predictPerOd++) {
						try {
//							System.out.println("from X " + fromX + " from Y " + fromY + " to X " + toX + " to Y " + toY);
							double startTime = 0;
							long Time1 = System.currentTimeMillis();
							Leg leg = this.fetch(fromX, fromY, toX, toY, startTime);
							List<Id<Link>> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
							long Time2 = System.currentTimeMillis();
							ArrayList<String> routeList = new ArrayList();
							ArrayList<Id> routeIdList = new ArrayList();
							for (Id<Link> linkid : linkIds) {
								routeList.add(linkid.toString());
								routeIdList.add(linkid);
							}

							if (routeList.size() > 0) {
//								saveRoute(routeList);
//								saveRouteToWKB(routeIdList, originNodeId, destinationNodeId, network);
								routes.add(routeIdList);
								routesODwise.add(routeIdList);
//								System.out.println("[i] computation time: " + timeDiff + " milliseconds");
								double distance = getRouteDistance(routeIdList, network);
								distances.add(distance);
								int linkCounter = countLinks(routeList);
								linkCounts.add(linkCounter);
//								double accuracy = calculateOverlapAccuracyTwoRoutes(observedRoutes.get(j).toString(), routeIdList, network);
//								accuracyODwise.add(accuracy);
//								SPoverlapsODwise.add(accuracy);
//								System.out.println("overlap: " + overlap);
							} else {
								System.out.println("[!] error");
							}
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("[!] connectivity error");
						}
						
						
					}
					
					accuracies.add(accuracyODwise.stream().mapToDouble(Double::doubleValue).average().orElse(0));

					linesRead += 1;
					System.out.println(
							"average SP overlap OD wise: " + SPoverlapsODwise.stream().mapToDouble(Double::doubleValue).average().orElse(0) + "%");
					System.out.println("weighted overlap: " + calculateOverlapMultipleRoutes(routesODwise, network));
					overlaps.add(calculateOverlapMultipleRoutes(routesODwise, network));
		            System.out.println("unique routes: " + countUniqueRoutes(routesODwise));	            
		            uniqueRoutes.add(countUniqueRoutes(routesODwise));
		            
				}
			}
//			saveRoutesToWKB(routes, network);
//			saveValidationSPRoutes(routes);		
//			enrichRoutes(routes, network);
//			saveRouteEdgesToCsv(routes, network);

			
//			calcFPRCurve(accuracies);
			System.out.println(
					"average length: " + distances.stream().mapToDouble(Double::doubleValue).average().orElse(0) + "m");
			System.out.println("std length: " + Math.sqrt(distances.stream().mapToDouble(
					x -> Math.pow(x - distances.stream().collect(Collectors.averagingDouble(Double::doubleValue)), 2))
					.average().orElse(0)));
			System.out.println(
					"average SP overlap: " + SPoverlaps.stream().mapToDouble(Double::doubleValue).average().orElse(0) + "%");
			System.out.println(
					"average overlap: " + overlaps.stream().mapToDouble(Double::doubleValue).average().orElse(0) + "%");
            System.out.println("average unique routes: " +  uniqueRoutes.stream().mapToDouble(Integer::doubleValue).average().orElse(0));
			getStatistics(distances);
			System.out.println(
					"average links: " + linkCounts.stream().mapToDouble(Integer::doubleValue).average().orElse(0));
			
			ArrayList<Double> compTimes = this.comptimes;
			System.out.println("average computation time: "
					+ compTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0) + " milliseconds");
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static int countLinks (ArrayList<String> links) {
		return links.size();
	}

	
	public static void getStatistics(ArrayList<Double> distances) {
		
        double[] arrayValues = new double[distances.size()];
        for (int i = 0; i < distances.size(); i++) {
            arrayValues[i] = distances.get(i);
        }

        // Calculate statistics
        double median = StatUtils.percentile(arrayValues, 50); // 50th percentile is the median
        double std = Math.sqrt(StatUtils.variance(arrayValues));
        
        // DescriptiveStatistics for percentiles
        DescriptiveStatistics stats = new DescriptiveStatistics(arrayValues);
        double percentile25 = stats.getPercentile(25);
        double percentile75 = stats.getPercentile(75);

        // Output the results
        System.out.println("Median: " + median);
        System.out.println("Standard Deviation: " + std);
        System.out.println("25th Percentile: " + percentile25);
        System.out.println("75th Percentile: " + percentile75);

	}
	
	
	public Leg fetch(float fromX, float fromY, float toX, float toY, double startTime) {

		Activity fromAct = PopulationUtils.createActivityFromCoord("h", new Coord(fromX, fromY));
		Facility fromFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(this.network, fromAct.getCoord()));

		Activity toAct = PopulationUtils.createActivityFromCoord("h", new Coord(toX, toY));
		Facility toFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(this.network, toAct.getCoord()));

		long Time1 = System.currentTimeMillis();
		List<? extends PlanElement> pes = this.router.calcRoute(fromFacility, toFacility, startTime, null);
		long Time2 = System.currentTimeMillis();
		double timeDiff = Time2 - Time1;
		this.comptimes.add(timeDiff);

		return (Leg) pes.get(0);
	}

	
	public static double getRouteDistance(ArrayList<Id> route, Network network_milos) {
		double distance = 0;
		for (Id id : route) {
			distance += network_milos.getLinks().get(id).getLength();
		}
		return distance;
	}

	
	public void saveRoute(ArrayList<String> route) {

		String originNodeId = route.get(0);
		String destinationNodeId = route.get(route.size() - 1);
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
		String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\sp_routes\\"
				+ originNodeId + "_" + destinationNodeId + "_" + timestamp + ".csv";
		try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
			writer.println("i,x,y");

			for (int i = 0; i <= route.size() - 1; i++) {
				String currentId = route.get(i);
				Id<Link> linkId = Id.createLinkId(currentId);
				writer.println(i + "," + this.network.getLinks().get(linkId).getToNode().getCoord().getX() + ","
						+ this.network.getLinks().get(linkId).getToNode().getCoord().getY());
			}
		} catch (IOException e) {
			System.err.println("Error writing CSV file: " + e.getMessage());
		}
	}

	
	public static double calculateAccucaryTwoRoutes (String observedLinks, ArrayList<Id> predictedRoute, Network network_milos) {
		double accuracy = 0;
        
        ArrayList<Id> observedLinkIds = new ArrayList<>();
        for (String link : observedLinks.split("\\.")) {
        	Id<Link> linkId = Id.createLinkId(link);
        	observedLinkIds.add(linkId);
        }
        
        double routeLength = getRouteDistance(observedLinkIds, network_milos);
		double overlapLength = 0;
        for (int ii = 0; ii < observedLinkIds.size() - 1; ii++) {
            String link1 = observedLinkIds.get(ii).toString();
            String nextLink1 = observedLinkIds.get(ii + 1).toString();

            // Iterate through each link in the second route
            for (int jj = 0; jj < predictedRoute.size() - 1; jj++) {
                String link2 = predictedRoute.get(jj).toString();
                String nextLink2 = predictedRoute.get(jj + 1).toString();

                // If there's an overlap between consecutive links in both routes
                if (link1.equals(link2) && nextLink1.equals(nextLink2)) {
                    // Query the length of the link using the network object
                    overlapLength += network_milos.getLinks().get(observedLinkIds.get(ii)).getLength();
                    break; // Break out of the inner loop since overlap found
                }
            }
        }
        double overlapShare = overlapLength / routeLength * 100;
//		System.out.println("accuracy: " + overlapShare + "%");
		return overlapShare;
	}
	
	
	public static double calculateOverlapAccuracyTwoRoutes (String observedLinksString, ArrayList<Id> predictedRoute, Network network_milos) {
		
		double accuracy = 0;
		double overlapLength = 0;
		
        ArrayList<String> observedLinks = new ArrayList<>();
        ArrayList<Id> observedLinkIds = new ArrayList<>();
        for (String link : observedLinksString.split("\\.")) {
        	observedLinks.add(link);
        	Id<Link> linkId = Id.createLinkId(link);
        	observedLinkIds.add(linkId);
        }
        
        ArrayList<String> predictedLinkIds = new ArrayList<>();
        for (Id link : predictedRoute) {
        	predictedLinkIds.add(link.toString());
        }
        
        for (String edgeId : predictedLinkIds) {
            if (observedLinks.contains(edgeId)) {
            	Id<Link> linkId = Id.createLinkId(edgeId);
                overlapLength += network_milos.getLinks().get(linkId).getLength();
            }
        }
        
        accuracy = overlapLength / getRouteDistance(observedLinkIds, network_milos) * 100;

		return accuracy;
	}
	
	
	public static double calculateOverlapMultipleRoutes (ArrayList<ArrayList<Id>> routes, Network network_milos) {
		double overlap = 0;
		ArrayList<Double> overlapShares = new ArrayList<>();
		ArrayList<Double> overlapShareWeights = new ArrayList<>();
		
		for (int i=0; i < routes.size(); i++) {	
			double routeLength = getRouteDistance(routes.get(i), network_milos);
			for (int j=0; j < routes.size(); j++) {
				
				if (i != j) {
					double overlapLength = 0;
			        for (int ii = 0; ii < routes.get(i).size() - 1; ii++) {
			            String link1 = routes.get(i).get(ii).toString();
			            String nextLink1 = routes.get(i).get(ii + 1).toString();

			            // Iterate through each link in the second route
			            for (int jj = 0; jj < routes.get(j).size() - 1; jj++) {
			                String link2 = routes.get(j).get(jj).toString();
			                String nextLink2 = routes.get(j).get(jj + 1).toString();

			                // If there's an overlap between consecutive links in both routes
			                if (link1.equals(link2) && nextLink1.equals(nextLink2)) {
			                    // Query the length of the link using the network object
			                    overlapLength += network_milos.getLinks().get(routes.get(i).get(ii)).getLength();
			                    break; // Break out of the inner loop since overlap found
			                }
			            }
			        }
			        double overlapShare = overlapLength / routeLength;
			        overlapShares.add(overlapShare);
			        overlapShareWeights.add(routeLength);			
				}			
			}	
		}
		
//		double weightedSum = 0;
//		double totalWeight = 0;
//        for (int i = 0; i < overlapShares.size(); i++) {
//            weightedSum += overlapShares.get(i) * overlapShareWeights.get(i);
//            totalWeight += overlapShareWeights.get(i);
//        }
//        return weightedSum / totalWeight;
        return overlapShares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
	}
	
	
	public static void calcFPRCurve (ArrayList<Double> accuracies) {
		
		System.out.println("FPR curve");
		double count10 = 0;
		double count20 = 0;
		double count30 = 0;
		double count40 = 0;
		double count50 = 0;
		double count60 = 0;
		double count70 = 0;
		double count80 = 0;
		double count90 = 0;
		double count100 = 0;
		
		
		for (double acc: accuracies) {
			if (acc >= 10) {
				count10 +=1; 
			}
			if (acc >= 20) {
				count20 +=1; 
			}
			if (acc >= 30) {
				count30 +=1; 
			}
			if (acc >= 40) {
				count40 +=1; 
			}
			if (acc >= 50) {
				count50 +=1; 
			}
			if (acc >= 60) {
				count60 +=1; 
			}
			if (acc >= 70) {
				count70 +=1; 
			}
			if (acc >= 80) {
				count80 +=1; 
			}
			if (acc >= 90) {
				count90 +=1; 
			}
			if (acc >= 100) {
				count100 +=1; 
			}
		}
		count10 = count10/accuracies.size();
		count20 = count20/accuracies.size();
		count30 = count30/accuracies.size();
		count40 = count40/accuracies.size();
		count50 = count50/accuracies.size();
		count60 = count60/accuracies.size();
		count70 = count70/accuracies.size();
		count80 = count80/accuracies.size();
		count90 = count90/accuracies.size();
		count100 = count100/accuracies.size();
		System.out.println("100:" + count100 + " 90:" + count90 + " 80:" + count80 + " 70:" + count70 + " 60:" + count60 + " 50:" + count50 + " 40:" + count40 + " 30:" + count30 + " 20:" + count20 + " 10:" + count10);
		
	}
	
	
	public static int countUniqueRoutes(ArrayList<ArrayList<Id>> routes) {
        HashSet noDupSet = new HashSet();
        for (ArrayList<Id> route : routes) {
        	String routeIdentifier = "";
        	for (Id id: route) {
        		routeIdentifier += id.toString();
        	}
        	noDupSet.add(routeIdentifier);
        }
        return noDupSet.size();
	}
	
	
	public static void enrichRoutes(ArrayList<ArrayList<Id>> routes, Network network_milos) {
		
		
		ArrayList<Double> bikeLaneShares = new ArrayList<>();
		ArrayList<Double> bikePathShares = new ArrayList<>();
		ArrayList<Double> slope1Shares = new ArrayList<>();
		ArrayList<Double> slope2Shares = new ArrayList<>();
		ArrayList<Double> slope3Shares = new ArrayList<>();
		ArrayList<Double> speedLimitShares = new ArrayList<>();
		ArrayList<Double> aadtShares = new ArrayList<>();
		
		
		for (ArrayList<Id> route: routes) {
			double routeLenght = getRouteDistance(route, network_milos);
			double lengthBikeLane = 0;
			double lengthBikePath = 0;
			double lengthSpeed = 0;
			String[] maxSpeedPatterns = {"30.0", "20.0", "15.0", "10.0", "5.0"};
			double lengthSlope1 = 0;
			double lengthSlope2 = 0;
			double lengthSlope3 = 0;
			double lengthAadt10 = 0;
			
			for (Id<Link> link: route) {
				
                if (network_milos.getLinks().get(link).getAttributes().getAttribute("lanes") != null) {
		            if (network_milos.getLinks().get(link).getAttributes().getAttribute("lanes").toString().indexOf("P") != -1) {
		            	lengthBikePath = lengthBikePath + network_milos.getLinks().get(link).getLength();
		            }					        	
		            if (network_milos.getLinks().get(link).getAttributes().getAttribute("lanes").toString().indexOf("L") != -1) {
		            	lengthBikeLane = lengthBikeLane + network_milos.getLinks().get(link).getLength();
		            }		  	
                }
                
                if (network_milos.getLinks().get(link).getAttributes().getAttribute("grade") != null) {
                	double grade = Double.parseDouble(network_milos.getLinks().get(link).getAttributes().getAttribute("grade").toString());
                	if (grade >= 0.02 && grade < 0.06) {
                		lengthSlope1 = lengthSlope1 + network_milos.getLinks().get(link).getLength();
                	}
                	if (grade >= 0.06 && grade < 0.1) {
                		lengthSlope2 = lengthSlope2 + network_milos.getLinks().get(link).getLength();
                	}
                	if (grade >= 0.1) {
                		lengthSlope3 = lengthSlope3 + network_milos.getLinks().get(link).getLength();
                	} 	
                }
                
                if (network_milos.getLinks().get(link).getAttributes().getAttribute("max_speed") != null) {
                    for (String pattern : maxSpeedPatterns) {
                        if (containsPattern(network_milos.getLinks().get(link).getAttributes().getAttribute("max_speed").toString(), pattern)) {
                        	lengthSpeed = lengthSpeed + network_milos.getLinks().get(link).getLength();;
                        }
                    }           	
                }
                
                if (network_milos.getLinks().get(link).getAttributes().getAttribute("aadt") != null) {
                	double aadt = Double.parseDouble(network_milos.getLinks().get(link).getAttributes().getAttribute("aadt").toString());
                	if (aadt >= 10000) {
                		lengthAadt10 = lengthAadt10 + network_milos.getLinks().get(link).getLength();
                	}
                }
                
				
			}
			
			double shareBikeLane = lengthBikeLane / routeLenght;
			double shareBikePath = lengthBikePath / routeLenght;
			double shareSlope1 = lengthSlope1 / routeLenght;
			double shareSlope2 = lengthSlope2 / routeLenght;
			double shareSlope3 = lengthSlope3 / routeLenght;
			double shareSpeedLimit = lengthSpeed / routeLenght;
			double shareAadt10 = lengthAadt10 / routeLenght;
			bikeLaneShares.add(shareBikeLane);
			bikePathShares.add(shareBikePath);
			slope1Shares.add(shareSlope1);
			slope2Shares.add(shareSlope2);
			slope3Shares.add(shareSlope3);
			speedLimitShares.add(shareSpeedLimit);
			aadtShares.add(shareAadt10);
			
		}
		
		double avgShareBikeLane = bikeLaneShares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgShareBikePath = bikePathShares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgShareSlope1 = slope1Shares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgShareSlope2 = slope2Shares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgShareSlope3 = slope3Shares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgSpeedLimit = speedLimitShares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double avgAadt = aadtShares.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		System.out.println("share bike lane: " + avgShareBikeLane);
		System.out.println("share bike path: " + avgShareBikePath);
		System.out.println("share slope1: " + avgShareSlope1);
		System.out.println("share slope2: " + avgShareSlope2);
		System.out.println("share slope3: " + avgShareSlope3);
		System.out.println("share speed limit: " + avgSpeedLimit);
		System.out.println("share aadt10: " + avgAadt);
	}
	
	
	public static void saveRoutesToWKB(ArrayList<ArrayList<Id>> routes, Network network_milos) {
		
//      List<Double[]> coordinates = new ArrayList<>();
//      for (Object id: route) {
//      	coordinates.add(new Double[]{network_milos.getLinks().get(id).getToNode().getCoord().getX(), network_milos.getLinks().get(id).getToNode().getCoord().getY()});
//      }
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
      String filePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\sp_routes\\wkb_routes_" + timestamp + ".csv";

      try (FileWriter writer = new FileWriter(filePath)) {
          // Write header
          writer.append("id;wkb\n");

          
          int index = 0;
          for (ArrayList<Id> route: routes) {
	            writer.append(index + ";LINESTRING(");
	            for (Id id : route) {
	                writer.append(Double.toString(network_milos.getLinks().get(id).getToNode().getCoord().getX()))
	                        .append(" ")
	                        .append(Double.toString(network_milos.getLinks().get(id).getToNode().getCoord().getY()))
	                        .append(",");
	                }
	            writer.append(")");
	            writer.append("\n");
	            index +=1;
          
          }

          System.out.println("WKB CSV file created successfully.");
      } catch (IOException e) {
          e.printStackTrace();
      }
	}

	
	public static void saveRouteToWKB(ArrayList<Id> route, Id originNodeId, Id destinationNodeId, Network network_milos) {
		
		  String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
	      String filePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\sp_routes\\wkb_route_" + originNodeId + "_" + destinationNodeId + "_" + timestamp + ".csv";

	      try (FileWriter writer = new FileWriter(filePath)) {
	          // Write header
	            writer.append("id;wkb\n");
		        writer.append("1;LINESTRING(");
		        for (Id id : route) {
		            writer.append(Double.toString(network_milos.getLinks().get(id).getToNode().getCoord().getX()))
		                    .append(" ")
		                    .append(Double.toString(network_milos.getLinks().get(id).getToNode().getCoord().getY()))
		                    .append(",");
		            }
		        writer.append(")");
		        writer.append("\n");

	          System.out.println("WKB CSV file created successfully.");
	      } catch (IOException e) {
	          e.printStackTrace();
	      }
		}

		
	public static void saveValidationSPRoutes(ArrayList<ArrayList<Id>> routes) {
		

		String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\validationSProutes.csv";
		try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
			writer.println("i,start_node,end_node,route");

			int j = 0;
			for (ArrayList<Id> route: routes) {
				String originNodeId = route.get(0).toString().split("_")[0];
				String destinationNodeId = route.get(route.size() - 1).toString().split("_")[1];
				String routeString = "";
				for (int i = 0; i <= route.size() - 1; i++) {
					String currentId = route.get(i).toString();
					routeString = routeString + "." + currentId;
				}
				j+=1;
				writer.println(j + "," + originNodeId + "," + destinationNodeId + "," + routeString.substring(1));
				
				
			}

		} catch (IOException e) {
			System.err.println("Error writing CSV file: " + e.getMessage());
		}
		
		
	}
	
	
	public static ArrayList<ArrayList<String>> readValidationOds() {
//		String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\validationSProutes.csv";
		String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\data\\validation\\validation_trips_female_ebike.csv";
		String column1Name = "start_node"; // Replace with the name of the first column
        String column2Name = "end_node"; // Replace with the name of the second column
        String column3Name = "route";
        ArrayList<String> column1 = new ArrayList();
        ArrayList<String> column2 = new ArrayList();
        ArrayList<String> column3 = new ArrayList();
        ArrayList<ArrayList<String>> returnList = new ArrayList();

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] header = reader.readNext(); // Read the header row
            int column1Index = -1;
            int column2Index = -1;
            int column3Index = -1;

            // Find the indices of the columns based on their names
            for (int i = 0; i < header.length; i++) {
                if (header[i].equals(column1Name)) {
                    column1Index = i;
                }
                if (header[i].equals(column2Name)) {
                    column2Index = i;
                }
                if (header[i].equals(column3Name)) {
                    column3Index = i;
                }
            }

            // Check if both columns are found in the header
            if (column1Index == -1 || column2Index == -1) {
                throw new IllegalArgumentException("One or both columns not found in the header");
            }

            // Read the data rows and extract the values of the specified columns
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                column1.add(nextLine[column1Index]);
                column2.add(nextLine[column2Index]);
                column3.add(nextLine[column3Index]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        returnList.add(column1);
        returnList.add(column2);
        returnList.add(column3);
        return returnList;
	}
	
	
	public static ArrayList<ArrayList<String>> readMappedOds() {
		String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\population\\population_male_ebike.csv";
        String column1Name = "origin_node"; // Replace with the name of the first column
        String column2Name = "destination_node"; // Replace with the name of the second column
        ArrayList<String> column1 = new ArrayList();
        ArrayList<String> column2 = new ArrayList();
        ArrayList<ArrayList<String>> returnList = new ArrayList();

        try (CSVReader reader = new CSVReader(new FileReader(csvFilePath))) {
            String[] header = reader.readNext(); // Read the header row
            int column1Index = -1;
            int column2Index = -1;

            // Find the indices of the columns based on their names
            for (int i = 0; i < header.length; i++) {
                if (header[i].equals(column1Name)) {
                    column1Index = i;
                }
                if (header[i].equals(column2Name)) {
                    column2Index = i;
                }
            }

            // Check if both columns are found in the header
            if (column1Index == -1 || column2Index == -1) {
                throw new IllegalArgumentException("One or both columns not found in the header");
            }

            // Read the data rows and extract the values of the specified columns
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                column1.add(nextLine[column1Index]);
                column2.add(nextLine[column2Index]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        returnList.add(column1);
        returnList.add(column2);
        return returnList;
	}
	
	
	public static void saveRouteEdgesToCsv(ArrayList<ArrayList<Id>> routes, Network network_milos) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\population\\edges_ssp_male_ebike_" + timestamp + ".csv";
        
        
	      try (FileWriter writer = new FileWriter(csvFilePath)) {
	          // Write header
	          writer.append("id; edges\n");

	          
	          int index = 0;
	          boolean isFirstIteration = true;
	          for (ArrayList<Id> route: routes) {
	        	  
	        	  String s = "";
	              
		          	for (int i = route.size() - 1; i >= 0; i--) {
		                  Id currentId = route.get(i);
		                  s += currentId;
		                  s += ",";
		                  
		          	}
		          	writer.append(String.valueOf(index) + ";" + s + "\n");
		          	index += 1;
	          
	          } 
	          System.out.println("Edges CSV file created successfully.");
	      } catch (IOException e) {
	          e.printStackTrace();
	      } 
    
	}
	
	
    public static boolean containsPattern(String input, String pattern) {
        // Compile the regular expression pattern
        Pattern regex = Pattern.compile(pattern);

        // Create a Matcher object
        Matcher matcher = regex.matcher(input);

        // Use Matcher's find() method to check if the pattern is found in the input string
        return matcher.find();
    } 
	
	
	public static void main(String[] args) {
		// folder where the MATSim files is stored
		String runInputFolder = args[0];
		// true: use congested times
		boolean calcCongestedTravelTimes = false;
		// file containing routes to route with columns [id, fromx, fromy, tox, toy,
		// departuretime]
		String odsFile = args[1];
		// file where to store the routed ODs
		String outputFile = args[2];

		// read network file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(runInputFolder + "/" + "network/dummy.xml");
		Network reducedNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(reducedNetwork, CollectionUtils.stringToSet(TransportMode.bike));

		AdrianSPRouter ods;
		if (calcCongestedTravelTimes) {
			String config = runInputFolder + "/" + "output_config.xml";
			String events = runInputFolder + "/" + "output_events.xml.gz";
			ods = new AdrianSPRouter(reducedNetwork, config, events);
		} else {
			// TODO: routing with freeflow travel times is not yet implemented!!
			String config = runInputFolder + "/" + "config.xml";
			ods = new AdrianSPRouter(reducedNetwork, config, null);
		}
		ods.run(odsFile, outputFile);

	}

}