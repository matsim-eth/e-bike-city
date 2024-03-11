package ebikecity.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;


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
import org.ojalgo.array.SparseArray;

import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;


public class RecursiveLogitMatLabRouter {
	
	private Network network; 
	
	
	public RecursiveLogitMatLabRouter (Network network_milos) {
		// constructor 
		
		this.network = network_milos;
		
		int numberOfLinks = 0;
		Map<Id<Link>, Integer> linkIdToIndexMap = new HashMap<>();
		Map<Id<Node>, Integer> nodeIdToIndexMap = new HashMap<>();
		ArrayList<Id> nodeIdList = new ArrayList<>();
		for (Link link : network_milos.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
		        Id<Link> linkId = link.getId();
		        Id<Node> nodeId = link.getToNode().getId();
		        if (!linkIdToIndexMap.containsKey(linkId)) {
		            linkIdToIndexMap.put(linkId, numberOfLinks++);
		        }
		        if (!nodeIdToIndexMap.containsKey(nodeId)) {
		        	nodeIdToIndexMap.put(nodeId, numberOfLinks);
		        	nodeIdList.add(nodeId);
		        }
			}
		}   
		
//		saveLinkMapToCSV(linkIdToIndexMap, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\hashmapLinks.csv");
//		saveNodeMapToCSV(nodeIdToIndexMap, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\hashmapNodes.csv");

		double paraLength = -0.01;
		double paraBikePath = 0.000;
		double paraBikeLane = 0.000;
		double paraBikePathMixed = 0.000;
		double paraUTurn = -1;
		double scaleFactor = 1;
		double downUtilScaleFactor = 1;
        ArrayList<Double> valueList = new ArrayList<>();
        ArrayList<Double> valueListExp = new ArrayList<>();
        ArrayList<Integer> rowIndicesList = new ArrayList<>();
        ArrayList<Integer> colIndicesList = new ArrayList<>();
        Map<String, Double> matrixMap = new HashMap<>();
        Map<String, Double> matrixMapExp = new HashMap<>();
		for (Link link : network_milos.getLinks().values()) {
		  	if (link.getAllowedModes().contains(TransportMode.car)) {
		  			Id<Node> fromNodeId = link.getFromNode().getId();
		            Id<Link> linkId = link.getId();
		            Set<Id<Link>> outgoingLinks = link.getToNode().getOutLinks().keySet();
		            for (Id<Link> outgoingLinkId : outgoingLinks) {
		            	if (network_milos.getLinks().get(outgoingLinkId).getAllowedModes().contains(TransportMode.car)) {
		            		
			                int outgoingLinkIndex = linkIdToIndexMap.get(outgoingLinkId);
			                int currentLinkIndex = linkIdToIndexMap.get(linkId);
			                rowIndicesList.add(currentLinkIndex + 1);
			                colIndicesList.add(outgoingLinkIndex + 1);
			                String matrixKey = currentLinkIndex + "," + outgoingLinkIndex;
			                double utility = network_milos.getLinks().get(outgoingLinkId).getLength() * paraLength;
			                if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes") != null) {
					            if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes").toString().indexOf("P") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikePath;
					            }					        	
					        
					            if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes").toString().indexOf("L") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikeLane;
					            }		
					            
					            if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes").toString().indexOf("X") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikePathMixed;
					            }		
			                }
			                if (network_milos.getLinks().get(outgoingLinkId).getToNode().getId().equals(fromNodeId)) {
			                	utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraUTurn;
			                }
			                
			                valueList.add(scaleFactor * utility);
			                valueListExp.add(Math.exp(scaleFactor * utility));
			                matrixMap.put(matrixKey, scaleFactor * utility);
			                matrixMapExp.put(matrixKey, Math.exp(scaleFactor * utility));
//			                valueList.add(utility);
//			                matrixMap.put(matrixKey, utility);
		        		}
		            }
		  		}		      
		  }

        
		int[] rowIndices = rowIndicesList.stream().mapToInt(Integer::intValue).toArray();
		int[] colIndices = colIndicesList.stream().mapToInt(Integer::intValue).toArray();
        double[] values = valueListExp.stream().mapToDouble(Double::doubleValue).toArray();	
		
		try {
			MatlabEngine matlab = MatlabEngine.startMatlab();
			
			System.out.println("MATLab engine started");
			
            matlab.putVariable("rowIndices", rowIndices);
            matlab.putVariable("colIndices", colIndices);
            matlab.putVariable("values", values);
            matlab.eval("M_sp = sparse(rowIndices, colIndices, values);"); // exp of the systematic utility matrix
            matlab.eval("[n m]= size(M_sp);");
//            matlab.eval("M_dest_full = full(M_sp)");
            
          
            long Time1 = System.currentTimeMillis();  
            
//            Random random = new Random();
//            int randomIndex1 = random.nextInt(nodeIdList.size());
//            Id<Node> destinationNodeId = nodeIdList.get(randomIndex1);
//            System.out.println(destinationNodeId);
//            int randomIndex2 = random.nextInt(nodeIdList.size());
//            Id<Node> originNodeId = nodeIdList.get(randomIndex2);
//            System.out.println(originNodeId);
            
//            Id<Node> destinationNodeId = Id.createNodeId("4926779092");
//            Id<Node> originNodeId = Id.createNodeId("794063724");
            
            Id<Node> destinationNodeId = Id.createNodeId("92509791");
            Id<Node> originNodeId = Id.createNodeId("241214622");
            
//            Id<Node> destinationNodeId = Id.createNodeId("3");
//            Id<Node> originNodeId = Id.createNodeId("1");
            
            StringBuilder rowIndicesString = new StringBuilder();
            for (Link l: network_milos.getNodes().get(destinationNodeId).getInLinks().values()) {
            	rowIndicesString.append(linkIdToIndexMap.get(l.getId())+1).append(","); // MATLab 1-based indexing
            }
            rowIndicesString.deleteCharAt(rowIndicesString.length() - 1);

            matlab.eval("M_dest = M_sp(1:n,1:n);");
            matlab.eval("M_dest(:,n+1) = 0;");
//            matlab.eval("M_dest(4,end) = 1;"); // no need for systematic, just 1
            matlab.eval("M_dest([" + rowIndicesString.toString() + "], end) = 1;");
            matlab.eval("M_dest(n+1,:) = zeros(1,n+1);");
            matlab.eval("M_dest_full = full(M_dest);");

            matlab.eval("[n m]= size(M_dest);");
            matlab.eval("b = zeros(n,1);");
            matlab.eval("b(n)=1;");
            matlab.eval("b_sp = sparse(b);");
            matlab.eval("I = speye(size(M_dest));");
            matlab.eval("A = I - M_dest;");
            matlab.eval("Z = mldivide(A, b_sp);");
//            matlab.eval("minele = min(Z);");
//            matlab.eval("boolV0 = 1;");
//            matlab.eval("if minele < -1e-3\n boolV0 = 0;\n end");   // NUM_ERROR = -1e-3;
            matlab.eval("Zabs = full(abs(Z));");
//            matlab.eval("resNorm = norm(A * Zabs - b_sp);");
//            matlab.eval("if resNorm > 1e-3\n boolV0 = 0;\n end");   // RESIDUAL  =  1e-3; 
            double[] downstreamUtility = matlab.getVariable("Zabs");
            
            long Time2 = System.currentTimeMillis();
            long timeDiff = Time2 - Time1;
            System.out.println("[i] Solving Ax=b: " + timeDiff + " milliseconds");   
            
            
            // loop through network starting with origin until destination reached
            ArrayList<Integer> routeNodeIds = new ArrayList<>();
            ArrayList<Id> routeLinkIds = new ArrayList<>();
            boolean isFirstIteration = true;
            boolean isFirstIteration2 = true;
            Id nextLinkId = null;
            Id currentInNodeId = null;
            int currentInNodeInLinkIndex = 0;
            while (true) {
            	if (isFirstIteration) {
            		currentInNodeId = originNodeId;
            		Map<Integer, Double> downstreamMapInitialLink = new HashMap<>();
            		
                	for (Link inLink : network_milos.getNodes().get(currentInNodeId).getInLinks().values()) {  
                		if (inLink.getAllowedModes().contains(TransportMode.car)) {
                			currentInNodeInLinkIndex = linkIdToIndexMap.get(inLink.getId()); // needs to be bike link (simplify with bike-only network)
                			
                			System.out.println("check: " + inLink.getId() + " index: " + currentInNodeInLinkIndex);
                			System.out.println("downstream util: " + downstreamUtility[currentInNodeInLinkIndex]);
                			downstreamMapInitialLink.put(currentInNodeInLinkIndex, downstreamUtility[currentInNodeInLinkIndex]);
                		}
                	}
                	 Entry<Integer, Double> maxEntry = Collections.max(downstreamMapInitialLink.entrySet(), Map.Entry.comparingByValue()); // start with link that has highest downstream utility (to avoid taking the one with the lowest and hence not chosing most direkt link as first path link)
                	 currentInNodeInLinkIndex = maxEntry.getKey();
                	 System.out.println("max downstreamutil index: " + currentInNodeInLinkIndex);
            		isFirstIteration = false;
            	} else {
            		currentInNodeId = network_milos.getLinks().get(nextLinkId).getToNode().getId();
            		currentInNodeInLinkIndex = linkIdToIndexMap.get(nextLinkId);
            	}
            	

//                int currentInNodeInLinkIndex = linkIdToIndexMap.get(network_milos.getNodes().get(currentInNodeId).getInLinks().values().iterator().next().getId());

      	
            	double denominator = 0;
                for (Link l: network_milos.getNodes().get(currentInNodeId).getOutLinks().values()) {
                	if (l.getAllowedModes().contains(TransportMode.car)) {
	                	int indexOutLink = linkIdToIndexMap.get(l.getId());
	                	String matrixKey = currentInNodeInLinkIndex + "," + indexOutLink;
	                	double sysUtil = matrixMapExp.get(matrixKey);
	                	double downstreamUtil = downUtilScaleFactor * downstreamUtility[indexOutLink];
//	                	double sysUtil = Math.exp(matrixMap.get(matrixKey));
//	                	double downstreamUtil = Math.exp(downUtilScaleFactor * downstreamUtility[indexOutLink]);
//	                	double downstreamUtil = Math.pow(downstreamUtility[indexOutLink], downUtilScaleFactor);
//	                	System.out.println("sysUtil: " + sysUtil);
//	                	System.out.println("downUtil: " + downstreamUtil);
	                	denominator = denominator + (downstreamUtil * sysUtil);
                	}
                }
                
                ArrayList<Double> downUtilValues = new ArrayList<>();
                ArrayList<Double> sysUtilValues = new ArrayList<>();
                ArrayList<Double> probValues = new ArrayList<>();
                ArrayList<Id> linkIds = new ArrayList<>();
                for (Link l: network_milos.getNodes().get(currentInNodeId).getOutLinks().values()) {
                	if (l.getAllowedModes().contains(TransportMode.car)) {
	                	int indexOutLink = linkIdToIndexMap.get(l.getId());
	                	String matrixKey = currentInNodeInLinkIndex + "," + indexOutLink;
	                	double sysUtil = matrixMapExp.get(matrixKey);
	                	double downstreamUtil = downUtilScaleFactor * downstreamUtility[indexOutLink];
//	                	double sysUtil = Math.exp(matrixMap.get(matrixKey));
//	                	double downstreamUtil = Math.exp(downUtilScaleFactor * downstreamUtility[indexOutLink]);
//	                	double downstreamUtil = Math.pow(downstreamUtility[indexOutLink], downUtilScaleFactor);
	                	double nominator = (downstreamUtil * sysUtil);
//	                	System.out.println("link id " + l.getId() + ", P= " + (nominator/denominator));
	                	System.out.println("link id " + l.getId() + ", SysUtil= " + sysUtil + ", DownUtil= " + downstreamUtil + ", No=" + nominator + ", Deno= " + denominator + ", P= " + (nominator/denominator));
	                	probValues.add((nominator/denominator));
	                	linkIds.add(l.getId());
	                	downUtilValues.add(downstreamUtil);
	                	sysUtilValues.add(sysUtil);
                	}
                }
                
//                nextLinkId = linkIds.get(findMaxDownUtilProbLink(sysUtilValues, downUtilValues));
//                nextLinkId = linkIds.get(sampleLink(probValues));
                nextLinkId = linkIds.get(choseMaxProbLink(probValues));
//                nextLinkId = linkIds.get(choseMaxProbLink(downUtilValues));
                
//                if (isFirstIteration2) {
//                	nextLinkId = linkIds.get(sampleLink(probValues));
//                	isFirstIteration2 = false;
//                } else {
//                	nextLinkId = linkIds.get(sampleLinkNoDoubleEntries(probValues, routeLinkIds, linkIds));
//                }

                System.out.println("chosen link: " + nextLinkId); 
                routeLinkIds.add(nextLinkId);
                Id nextLinkToNodeId = network_milos.getLinks().get(nextLinkId).getToNode().getId();
                if (nextLinkToNodeId.equals(destinationNodeId)) {
                	System.out.println("destination found"); 
                	System.out.println("number of links: " + routeLinkIds.size()); 
                	System.out.println("number of unique links: " + getUniqueLinks(routeLinkIds)); 
                	break;
                }
            	
            }
            long Time3 = System.currentTimeMillis(); 
            long timeDiff2 = Time3 - Time2;
            System.out.println("[i] Looping through network: " + timeDiff2 + " milliseconds"); 
            System.out.println("[i] Total time: " + Math.addExact(timeDiff, timeDiff2) + " milliseconds"); 
            
			
			matlab.close();
			
			System.out.println("MATLab engine closed");
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
        
	}  
	
	public static <T> int getUniqueLinks(ArrayList<Id> routeLinkIds) {
        Set<T> uniqueSet = new HashSet<>(routeLinkIds);
        return uniqueSet.size();
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

    public static int choseMaxProbLink(ArrayList<Double> probabilities) {
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
       
    public static int sampleLinkNoDoubleEntries(ArrayList<Double> probabilities, ArrayList<Id> previousLinkIds, ArrayList<Id> linkIds) {
        Random rand = new Random();
        double randomValue = rand.nextDouble(); // Generate a random number between 0 and 1
        double cumulativeProbability = 0.0;
        int index = 0;
        int returnIndex = 0;

        for (int i = 0; i < probabilities.size(); i++) {
            cumulativeProbability += probabilities.get(i);
            if (randomValue <= cumulativeProbability) {
            	index = i;
            	returnIndex = i;
            	break;
            }
        }
        
        if (previousLinkIds.contains(linkIds.get(index))) {
        	if (probabilities.size() != 1) {
        		if (returnIndex - 1 == -1) {
        			returnIndex = 1;
        		} else {
        			returnIndex = returnIndex - 1;
        		}
        	}
        }
        
        if (previousLinkIds.contains(linkIds.get(returnIndex))) {
        	System.out.println("double entry");
        }
        
        return returnIndex;
    }
    
	private static void saveLinkMapToCSV(Map<Id<Link>, Integer> map, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Writing header (optional)
            writer.println("LinkId, Index");

            // Writing data
            for (Map.Entry<Id<Link>, Integer> entry : map.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }

            System.out.println("CSV file saved successfully!");
        } catch (IOException e) {
            System.err.println("Error saving CSV file: " + e.getMessage());
        }
	}
	
	private static void saveNodeMapToCSV(Map<Id<Node>, Integer> map, String filePath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Writing header (optional)
            writer.println("NodeId, Index");

            // Writing data
            for (Map.Entry<Id<Node>, Integer> entry : map.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue());
            }

            System.out.println("CSV file saved successfully!");
        } catch (IOException e) {
            System.err.println("Error saving CSV file: " + e.getMessage());
        }
	}
	
    private static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(matrix[i][j] + "\t");
            }
            System.out.println();
        }
    }
            
	public static void main (String[] args) {
		// main method
		
		Config config = ConfigUtils.createConfig(); 
		
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork()); 
		
		reader.readFile(args[0]);
		
		RecursiveLogitMatLabRouter my_new_router = new RecursiveLogitMatLabRouter(scenario.getNetwork());
		
		//predictRoute(network);
		
		scenario.getNetwork().getLinks().values().toArray();
		

	}
	
	

}
