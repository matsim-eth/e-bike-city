package ebikecity.utils.probabilisticrouting_prod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;


/**
 * Recursive Logit router implementation for MATSim. See https://www.research-collection.ethz.ch/handle/20.500.11850/689046
 * main components:
 * 		1. calculation of systematic utilities (based on estimated parameters, see getSystematicUtility())
 * 		2. calculation of directional systematic utilities (same as above but considering any potential "previous links" for u-turn penalty)
 * 		1. & 2. are independent of considered OD pair
 * 		3. prediction of route for a given OD pair
 * 		3.1 calculation of downstream utilities (destination specific) using backwards iteration from D to O using BFS search. BFS needed to account for u-turns, otherwise backwards SP can be used (see paper). Early stop parameter can be specified to avoid enumerating the whole graph.
 * 		3.2 looping from O to D while sampling at each intersection using the probabilities defined through systematic & downstream utility
 *
 * @author Adrian Meister, IVT ETH Zuerich, 2024
 */

public class RecursiveLogitRouter {
	
	private Network network; 
	private int failedRoutingCounter;
	private int networkErrorCounter;
	private int loopingCounter;
	private int otherCounter;
	private ArrayList<Double> iteratingTimes;
	private ArrayList<Double> processTimes;
	
	public RecursiveLogitRouter (Network network_milos) {
		
		Config config = ConfigUtils.loadConfig("C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\config.xml");
		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.configure(config.travelTimeCalculator());

		// constructor 
		this.failedRoutingCounter = 0;
		this.loopingCounter = 0;
		this.networkErrorCounter = 0;
		this.otherCounter = 0;
		this.iteratingTimes = new ArrayList<Double>();
		this.processTimes = new ArrayList<Double>();
		
		ArrayList<Id> nodeIds = getNodeIds(network_milos);
		Map<Id<Link>, Double> systematicUtilities = getSystematicUtility(network_milos);
		Map<String, Double> systematicUtilitiesDirectional = getDirectionalSystematicUtility(network_milos);
        
        ArrayList<ArrayList<Id>> routes = new ArrayList<>();
        ArrayList<Double> compTimes = new ArrayList<>();
        	
        	// random O&D
//          Random random = new Random();
//          int randomIndex1 = random.nextInt(nodeIds.size());
//          Id<Node> destinationNode = nodeIds.get(randomIndex1);
//          int randomIndex2 = random.nextInt(nodeIds.size());
//          Id<Node> originNode = nodeIds.get(randomIndex2);
//          System.out.println("from: "+ originNode + ", to: " + destinationNode);
        
        	// specific O&D based in ID
          Id<Node> originNodeId = Id.createNodeId("4075");
          Id<Node> destinationNodeId = Id.createNodeId("17377");
          System.out.println("from: "+ originNodeId + ", to: " + destinationNodeId);
          
          ArrayList<ArrayList<Id>> routesODwise = new ArrayList<>();
          
          for (int predictPerOd = 1; predictPerOd < 2; predictPerOd++) {
        	  ArrayList<Id> routeLinkIds = null;
              long Time1 = System.currentTimeMillis();
              
              try {
            	  routeLinkIds = predictRoute(originNodeId, destinationNodeId, network_milos, systematicUtilitiesDirectional, systematicUtilities, -1.5, 50);
            	  }  catch (Exception e) {
            	  System.out.println("[!] Other problem");
            	  otherCounter +=1;
	              }
              double Time2 = System.currentTimeMillis(); 
              double timeDiff = Time2 - Time1;
              if (routeLinkIds != null) {      	  
	          	  compTimes.add(timeDiff);
	          	  routes.add(routeLinkIds);
	          	  routesODwise.add(routeLinkIds);
              }
            }

        System.out.println("average process time: " + processTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0) + " milliseconds");
        System.out.println("average computation time: " + compTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0) + " milliseconds");
        System.out.println("average downstream calc time: " + iteratingTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0) + " milliseconds");
        System.out.println("number of ODs with no downstream signal: " + this.failedRoutingCounter);
        System.out.println("number of ODs with infinite looping: " + this.loopingCounter);
        System.out.println("number of ODs with connectivity issues: " + this.networkErrorCounter);
        System.out.println("number of ODs with other issues: " + otherCounter);
	} 
	
	
	public static Map<Id<Link>, Double> getSystematicUtility (Network network_milos) {
		
		// male & bike
//		double paraLength = -0.023; 
//		double paraBikePath = 0.0017;
//		double paraBikeLane = 0.0032;
//		double paraSpeedLimit = 0.0013;
//		double paraGrade1 = 0.0007;
//		double paraGrade2 = -0.0012;
//		double paraGrade3 = -0.0177;
		
		// male & ebike
//		double paraLength = -0.023;   
//		double paraBikePath = 0.002; 
//		double paraBikeLane = 0.0026;
//		double paraSpeedLimit = 0.009; 
//		double paraGrade1 = 0.0021; 
//		double paraGrade2 = 0.0032; 
//		double paraGrade3 = -0.0129;
		
		// female & bike
//		double paraLength = -0.024;
//		double paraBikePath = 0.0018;
//		double paraBikeLane = 0.003;
//		double paraSpeedLimit = 0.0015;
//		double paraGrade1 = 0.0014;
//		double paraGrade2 = -0.0004;
//		double paraGrade3 = -0.0072;
		
		// female & ebike
//		double paraLength = -0.023;  
//		double paraBikePath = 0.0023;
//		double paraBikeLane = 0.0026;
//		double paraSpeedLimit = 0.0013;
//		double paraGrade1 = 0.0011;
//		double paraGrade2 = -0.001;
//		double paraGrade3 = -0.0152;
		
		double paraLength = -0.023; 
		double paraBikePath = 0.0017;
		double paraBikeLane = 0.0032;
		double paraSpeedLimit = 0.0013;
		double paraGrade1 = 0.0007;
		double paraGrade2 = -0.0012;
		double paraGrade3 = -0.0177;
		double paraScale = 1.0;
		String[] maxSpeedPatterns = {"30.0", "20.0", "15.0", "10.0", "5.0"};
		
		Map<Id<Link>, Double> systematicUtilities = new HashMap<>();
		
		for (Link link : network_milos.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.bike)) {
		        Id<Link> linkId = link.getId();
		        if (!systematicUtilities.containsKey(linkId)) {

		        	double sysUtility = network_milos.getLinks().get(linkId).getLength() * paraLength;
		        	
	                if (network_milos.getLinks().get(linkId).getAttributes().getAttribute("lanes") != null) {
			            if (network_milos.getLinks().get(linkId).getAttributes().getAttribute("lanes").toString().indexOf("P") != -1) {
			            	sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraBikePath;
			            }					        	
			            if (network_milos.getLinks().get(linkId).getAttributes().getAttribute("lanes").toString().indexOf("L") != -1) {
			            	sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraBikeLane;
			            }		
	                }
	                
	                if (network_milos.getLinks().get(linkId).getAttributes().getAttribute("grade") != null) {
	                	double grade = Double.parseDouble(network_milos.getLinks().get(linkId).getAttributes().getAttribute("grade").toString());
	                	if (grade >= 0.02 && grade < 0.06) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraGrade1;
	                	}
	                	if (grade >= 0.06 && grade < 0.1) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraGrade2;
	                	}
	                	if (grade >= 0.1) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraGrade3;
	                	}         	
	                }
	                
	                if (network_milos.getLinks().get(linkId).getAttributes().getAttribute("max_speed") != null) {
	                    for (String pattern : maxSpeedPatterns) {
	                        if (containsPattern(network_milos.getLinks().get(linkId).getAttributes().getAttribute("max_speed").toString(), pattern)) {
	                        	sysUtility = sysUtility + network_milos.getLinks().get(linkId).getLength() * paraSpeedLimit;;
	                        }
	                    }           					        	 	
	                }
		        	
		        	systematicUtilities.put(linkId, sysUtility * paraScale);
//		        	System.out.println("link:" + linkId + " SU:" + link.getLength() * paraLength);
		        }
			}
		} 
		
		return systematicUtilities;
	}
	
	
	public static Map<String, Double> getDirectionalSystematicUtility (Network network_milos) {
		
		// male & bike
//		double paraLength = -0.023; 
//		double paraBikePath = 0.0017;
//		double paraBikeLane = 0.0032;
//		double paraSpeedLimit = 0.0013;
//		double paraGrade1 = 0.0007;
//		double paraGrade2 = -0.0012;
//		double paraGrade3 = -0.0177;
		
		// male & ebike
//		double paraLength = -0.023;   
//		double paraBikePath = 0.002; 
//		double paraBikeLane = 0.0026;
//		double paraSpeedLimit = 0.009; 
//		double paraGrade1 = 0.0021; 
//		double paraGrade2 = 0.0032; 
//		double paraGrade3 = -0.0129;
		
		// female & bike
//		double paraLength = -0.024;
//		double paraBikePath = 0.0018;
//		double paraBikeLane = 0.003;
//		double paraSpeedLimit = 0.0015;
//		double paraGrade1 = 0.0014;
//		double paraGrade2 = -0.0004;
//		double paraGrade3 = -0.0072;
		
		// female & ebike
//		double paraLength = -0.023;  
//		double paraBikePath = 0.0023;
//		double paraBikeLane = 0.0026;
//		double paraSpeedLimit = 0.0013;
//		double paraGrade1 = 0.0011;
//		double paraGrade2 = -0.001;
//		double paraGrade3 = -0.0152;
		
		double paraLength = -0.023; 
		double paraBikePath = 0.0017;
		double paraBikeLane = 0.0032;
		double paraSpeedLimit = 0.0013;
		double paraGrade1 = 0.0007;
		double paraGrade2 = -0.0012;
		double paraGrade3 = -0.0177;
		double paraUTurn = -1.5;
		double paraScale = 1.0; 
		String[] maxSpeedPatterns = {"30.0", "20.0", "15.0", "10.0", "5.0"};

		Map<String, Double> sysUtilities = new HashMap<>();
		
		for (Link link : network_milos.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.bike)) { 
				
				Id toNode = network_milos.getLinks().get(link.getId()).getToNode().getId();
				Id fromNode = network_milos.getLinks().get(link.getId()).getFromNode().getId();
//				System.out.println("link:" + link.getId() + " toNode:" + toNode + " fromNode:" + fromNode);
	
				for (Id outLinkId: network_milos.getNodes().get(toNode).getOutLinks().keySet()) {
					Id outLinkToNode = network_milos.getLinks().get(outLinkId).getToNode().getId();
					Id outLinkFromNode = network_milos.getLinks().get(outLinkId).getFromNode().getId();
//					System.out.println("outLink:" + outLinkId + " toNode:" + outLinkToNode + " fromNode:" + outLinkFromNode);
					String matrixKey = link.getId() + "," + outLinkId;
//					System.out.println("matrixkey: " + matrixKey);
					double sysUtility = network_milos.getLinks().get(outLinkId).getLength() * paraLength;
					
	                if (network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("lanes") != null) {
			            if (network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("lanes").toString().indexOf("P") != -1) {
			            	sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraBikePath;
			            }					        	
			            if (network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("lanes").toString().indexOf("L") != -1) {
			            	sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraBikeLane;
			            }		
	                }
	                
	                if (network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("grade") != null) {
	                	double grade = Double.parseDouble(network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("grade").toString());
	                	if (grade >= 0.02 && grade < 0.06) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraGrade1;
	                	}
	                	if (grade >= 0.06 && grade < 0.1) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraGrade2;
	                	}
	                	if (grade >= 0.1) {
	                		sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraGrade3;
	                	}
			            	
	                }
	                
	                if (network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("max_speed") != null) {
			            
	                    for (String pattern : maxSpeedPatterns) {
	                        if (containsPattern(network_milos.getLinks().get(outLinkId).getAttributes().getAttribute("max_speed").toString(), pattern)) {
	                        	sysUtility = sysUtility + network_milos.getLinks().get(outLinkId).getLength() * paraSpeedLimit;;
	                        }
	                    }           					        	 	
	                }

					if (toNode == outLinkFromNode && fromNode == outLinkToNode) {
						sysUtility += network_milos.getLinks().get(outLinkId).getLength() * paraUTurn;
					}

					sysUtilities.put(matrixKey, sysUtility * paraScale);
//					System.out.println("matrixKey:" + matrixKey + " sustUtil:" + sysUtility);
				}
			}
		} 
		return sysUtilities;
	}
	
	
	public Map<Id<Link>, Double> calcDownstreamUtilities(Id originNodeId, Id destinationNodeId, Network network_milos, Map<Id<Link>, Double> systematicUtilities, Double paraUTurn, Integer surplusIterations) {
	      	      
		  int surplusIterationCounter = 0;
	      Map<Id<Link>, Double> downstreamUtilities = new HashMap<>();
	      
	      for (Link link : network_milos.getLinks().values()) {
	    	  if (network_milos.getLinks().get(link.getId()).getAllowedModes().contains(TransportMode.bike)) {
	    		  downstreamUtilities.put(link.getId(), Double.NEGATIVE_INFINITY);
	    	  }
	      }
	      
	      boolean originNotFound = true;
	      boolean originFound = false;
	      Set<NodeLinkPair> visitedPairs = new HashSet<>();
	      
	      Deque<NodeLinkPair> queue = new ArrayDeque<>();
	      NodeLinkPair destinationPair = new NodeLinkPair(destinationNodeId, null);
	      queue.add(destinationPair);
	      
	      NodeLinkPair firstPolledPair = queue.poll();
	      Id firstCurrentNodeId = firstPolledPair.getNodeId();
		  for (Link inLink: network_milos.getNodes().get(firstCurrentNodeId).getInLinks().values()) {
			  downstreamUtilities.put(inLink.getId(), 0.0);
			  queue.add(new NodeLinkPair(inLink.getFromNode().getId(), inLink.getId()));
		  }
	      
	      while (!queue.isEmpty()) {
	          
	          NodeLinkPair polledPair = queue.poll();
	    	  
	    	  if (visitedPairs.contains(polledPair)) {  // check if slow
	    		  continue;
	    	  }
	    	  
	    	  Id currentNodeId = polledPair.getNodeId();
	    	  Id previousLinkId = polledPair.getLinkId();
	    	  
	    	  if (currentNodeId.equals(originNodeId)) {
	    		  originFound = true;
	    		  originNotFound = false;
	    	  }
	    	  
              if (originFound) {
                  surplusIterationCounter++;
                  if (surplusIterationCounter > surplusIterations) {
                      break; 
                  }
              }
	    	  
    		  for (Link inLink: network_milos.getNodes().get(currentNodeId).getInLinks().values()) {
    			  if (network_milos.getLinks().get(inLink.getId()).getAllowedModes().contains(TransportMode.bike)) { 				 
    				  
	    			  Double downUtil = downstreamUtilities.get(previousLinkId) + systematicUtilities.get(previousLinkId);
	    			  
	    			  if (inLink.getFromNode().getId().equals(network_milos.getLinks().get(previousLinkId).getToNode().getId())) {
	    				  downUtil += network_milos.getLinks().get(previousLinkId).getLength() * paraUTurn;
	    			  }
	    			  
	    			  if (downUtil > downstreamUtilities.get(inLink.getId())) {
	    				  downstreamUtilities.put(inLink.getId(), downUtil);
	//    				  System.out.println("LOGGED: link:" + inLink.getId() + " DU:" + downUtil);
	    			  }
	    			  NodeLinkPair newPair = new NodeLinkPair(inLink.getFromNode().getId(), inLink.getId());
	//	    			  if (!visitedPairs.contains(newPair)) { // check whats faster
	    			  queue.add(newPair); 
	    			  
	    		  }
    		  }
	      
	    	 
	    	  visitedPairs.add(polledPair);
//              iterationCounter++; 
	      }

//          for (Id<Link> entry: downstreamUtilities.keySet()) {
//        	  System.out.println("link:" + entry + " DU:" + downstreamUtilities.get(entry));
//          }
          
        return downstreamUtilities;
		}
			

	public static ArrayList<Id> getNodeIds (Network network_milos) {
		ArrayList<Id> nodeIdList = new ArrayList<>();
		for (Link link : network_milos.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.bike)) {
		        Id<Node> nodeId = link.getToNode().getId();
		        nodeIdList.add(nodeId);
		        }
			} 
		return nodeIdList;
	}
	
		
	public ArrayList<Id> predictRoute(Id originNodeId, Id destinationNodeId, Network network_milos, Map<String, Double> systematicUtilitiesDirectional, Map<Id<Link>, Double> systematicUtilities, Double paraUTurn, Integer para){

		double Time1 = System.currentTimeMillis(); 
        Map<Id<Link>, Double> downstreamUtilities = calcDownstreamUtilities(originNodeId, destinationNodeId, network_milos, systematicUtilities, paraUTurn, para);
        double Time2 = System.currentTimeMillis(); 
        double timeDiffiterating = Time2 - Time1;
        iteratingTimes.add(timeDiffiterating);
		double mu = 1; // scaling factor
		
		// check OD pair connectivity
        if (network_milos.getNodes().get(destinationNodeId).getInLinks().values().size() == 0) {
        	System.out.println("[!] destination node disconnected"); 
        	this.networkErrorCounter += 1; 
        	return null;
        }
        
        // derive virtual starting link with max downstream utility 
        double maxInitialDownstreamUtil = 0;
        Id maxInitialDownstreamUtilId = null;
        for (Link originInLink: network_milos.getNodes().get(originNodeId).getInLinks().values()) {
        	if (originInLink.getAllowedModes().contains(TransportMode.bike)) {
	        	double initialDownstreamUtil = downstreamUtilities.get(originInLink.getId());
	        	if (initialDownstreamUtil < maxInitialDownstreamUtil) {
	        		maxInitialDownstreamUtil = initialDownstreamUtil;
	        		maxInitialDownstreamUtilId = originInLink.getId();
	        	}
        	}
        }

        ArrayList<Id> routeLinkIds = new ArrayList<>();
        Id nextLinkId = null;
        Id previousLinkId = maxInitialDownstreamUtilId;
        Id currentInNodeId = originNodeId;
        int iterationCounter = 0;
        
        // loop through network starting from O until D reached. Infinite looping error if D not found after 5000 links.
        while (true) {
        		
        	System.out.println("current node: " + currentInNodeId);
        	System.out.println("previous link: " + previousLinkId);
        	Map<Id<Link>, Double> utilities = new HashMap<>();
            boolean isFirstIteration = true;
            double maxUtil = 0;
        	
            // derive utilities for each outgoing link
            for (Link l: network_milos.getNodes().get(currentInNodeId).getOutLinks().values()) {  	
            	if (l.getAllowedModes().contains(TransportMode.bike)) {
            		String matrixKey = previousLinkId + "," + l.getId();
                	double sysUtil = systematicUtilitiesDirectional.get(matrixKey) * (1/mu);
                	double downstreamUtil = downstreamUtilities.get(l.getId()) ;
                	double utility = (sysUtil + downstreamUtil);
                	System.out.println("link: " + l.getId() + " sysUtil:" + sysUtil + " downUtil:" + downstreamUtil + " Util:" + utility);
                	utilities.put(l.getId(), utility);
                	
                	if (isFirstIteration) {
                		maxUtil = utility;
                		isFirstIteration = false;
                	}
                	
                	if (utility > maxUtil) {  // < uses MinUtil   > uses MaxUtil
                		maxUtil = utility;
                	}
            	}
            }

            for (Entry<Id<Link>, Double> entry : utilities.entrySet()) {
            	utilities.put(entry.getKey(), Math.exp((entry.getValue() + Math.abs(maxUtil))));
            }
            
            double denominator = 0;
            for (Entry<Id<Link>, Double> entry : utilities.entrySet()) {
            	denominator += entry.getValue();
            }
            
            // derive probabilities for each outgoing link
            ArrayList<Double> probValues = new ArrayList<>();
            ArrayList<Id> linkIds = new ArrayList<>();
            for (Entry<Id<Link>, Double> entry : utilities.entrySet()) {
            	System.out.println("link:" + entry.getKey() + " prob:" + (entry.getValue()/denominator));
                probValues.add((entry.getValue()/denominator));
            	linkIds.add(entry.getKey());
            }

            nextLinkId = linkIds.get(sampleLink(probValues)); // probabilistic sampling
//            nextLinkId = linkIds.get(chooseMaxProbLink(probValues)); // choose link with max utility without sampling
            System.out.println("chosen link: " + nextLinkId); 
            routeLinkIds.add(nextLinkId);
            Id nextLinkToNodeId = network_milos.getLinks().get(nextLinkId).getToNode().getId();
            if (nextLinkToNodeId.equals(destinationNodeId)) {
            	System.out.println("[*] destination found"); 
            	break;
            }
              
            iterationCounter += 1;
            if (iterationCounter > 5000) {
            	this.loopingCounter += 1;
            	System.out.println("infinite looping");
            	return null;
            }
            
            currentInNodeId = nextLinkToNodeId;
            previousLinkId = nextLinkId;
        }
        
		return routeLinkIds;
	}
	

    public static int findMaxDownUtilProbLink(ArrayList<Double> sysUtilValues, ArrayList<Double> downUtilValues) {

        int ignoreIndex = -1; 
        double highestValue = Double.MIN_VALUE;
        int highestIndex = -1;
        
        for (int i = 0; i < sysUtilValues.size(); i++) {
            if (sysUtilValues.get(i) == 0.0) {
            	ignoreIndex = i;
            }
        }
        
        for (int j = 0; j < downUtilValues.size(); j++) {
            // Skip the predefined index position
            if (j == ignoreIndex) {
                continue;
            }

            // Check if the current value is higher than the highestValue
            if (downUtilValues.get(j) > highestValue) {
            	highestValue = downUtilValues.get(j);
                highestIndex = j;
            }
        }
        
    	
        return highestIndex;
    }

    
    public static int sampleLink(ArrayList<Double> probabilities) {
        Random rand = new Random();
        double randomValue = rand.nextDouble(); // Generate a random number between 0 and 1
        double cumulativeProbability = 0.0;

        for (int i = 0; i < probabilities.size(); i++) {
            cumulativeProbability += probabilities.get(i);

            if (randomValue <= cumulativeProbability) {
                return i; // Return the index of the chosen alternative
            }
        }

        // This should not happen, but if it does, return the last index as a fallback
        return probabilities.size() - 1;
    }

    
    public static int chooseMaxProbLink(ArrayList<Double> probabilities) {
        if (probabilities.isEmpty()) {
            // Handle the case when the ArrayList is empty
            return -1; // You can choose another value or throw an exception
        }

        double maxValue = probabilities.get(0);
        int maxIndex = 0;

        for (int i = 1; i < probabilities.size(); i++) {
            if (probabilities.get(i) > maxValue) {
                maxValue = probabilities.get(i);
                maxIndex = i;
            }
        }

        return maxIndex;
    }  
  
       
    public static boolean containsPattern(String input, String pattern) {
        // Compile the regular expression pattern
        Pattern regex = Pattern.compile(pattern);

        // Create a Matcher object
        Matcher matcher = regex.matcher(input);

        // Use Matcher's find() method to check if the pattern is found in the input string
        return matcher.find();
    } 
 
    
	public static void main (String[] args) {
		
		Config config = ConfigUtils.createConfig(); 
		
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork()); 
		
		reader.readFile(args[0]);
		
		RecursiveLogitRouter my_new_router = new RecursiveLogitRouter(scenario.getNetwork());
		
		scenario.getNetwork().getLinks().values().toArray();
		
	}
	
}
