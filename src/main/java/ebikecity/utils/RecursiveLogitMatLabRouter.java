package ebikecity.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
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
import org.ojalgo.array.SparseArray;

import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;
import com.mathworks.engine.MatlabExecutionException;
import com.mathworks.engine.MatlabSyntaxException;


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
			if (link.getAllowedModes().contains(TransportMode.bike)) {
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
		
		
		double paraLength = -0.07;   // -0.03 full network
		double paraBikePath = 0.02;
		double paraBikeLane = 0.01;
		double paraSpeedLimit = 0.009;
		double paraGrade1 = 0.01; //0.01;
		double paraGrade2 = -0.03; //-0.03
		double paraGrade3 = -0.1; //-0.1;
		double paraUTurn = -1;
		double scaleFactor = 1;
		double scaleConstant = 0;
        ArrayList<Double> valueList = new ArrayList<>();
        ArrayList<Double> valueListExp = new ArrayList<>();
        ArrayList<Integer> rowIndicesList = new ArrayList<>();
        ArrayList<Integer> colIndicesList = new ArrayList<>();
        Map<String, Double> matrixMap = new HashMap<>();
        Map<String, Double> matrixMapExp = new HashMap<>();
        String[] maxSpeedPatterns = {"30.0", "20.0", "15.0", "10.0", "5.0"};
		for (Link link : network_milos.getLinks().values()) {
		  	if (link.getAllowedModes().contains(TransportMode.bike)) {
		  			Id<Node> fromNodeId = link.getFromNode().getId();
		            Id<Link> linkId = link.getId();
		            Set<Id<Link>> outgoingLinks = link.getToNode().getOutLinks().keySet();
		            for (Id<Link> outgoingLinkId : outgoingLinks) {
		            	if (network_milos.getLinks().get(outgoingLinkId).getAllowedModes().contains(TransportMode.bike)) {
		            		
			                int outgoingLinkIndex = linkIdToIndexMap.get(outgoingLinkId);
			                int currentLinkIndex = linkIdToIndexMap.get(linkId);
			                rowIndicesList.add(currentLinkIndex + 1);
			                colIndicesList.add(outgoingLinkIndex + 1);
			                String matrixKey = currentLinkIndex + "," + outgoingLinkIndex;
			                double utility = network_milos.getLinks().get(outgoingLinkId).getLength() * paraLength;
			                if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("lanes") != null) {
					            if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("lanes").toString().indexOf("P") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikePath;
					            }					        	
					        
					            if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("lanes").toString().indexOf("L") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikeLane;
					            }		
					            	
			                }
			                
			                if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("grade") != null) {
			                	double grade = Double.parseDouble(network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("grade").toString());
			                	if (grade >= 0.02 && grade < 0.06) {
			                		utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraGrade1;
			                	}
			                	if (grade >= 0.06 && grade < 0.1) {
			                		utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraGrade2;
			                	}
			                	if (grade >= 0.1) {
			                		utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraGrade3;
			                	}
					            	
			                }
			                
			                if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("max_speed") != null) {
					            
			                    for (String pattern : maxSpeedPatterns) {
			                        if (containsPattern(network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("max_speed").toString(), pattern)) {
			                        	utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraSpeedLimit;;
			                        }
			                    }           	
			                	if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("max_speed").toString().indexOf("P") != -1) {
					                utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikePath;
					            
					            }					        	
					        	
			                }
			                if (network_milos.getLinks().get(outgoingLinkId).getToNode().getId().equals(fromNodeId)) {
			                	utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraUTurn;
			                }
			                
			                valueList.add(scaleFactor * utility + scaleConstant);
			                valueListExp.add(Math.exp(scaleFactor * utility + scaleConstant));
			                matrixMap.put(matrixKey, scaleFactor * utility + scaleConstant);
			                matrixMapExp.put(matrixKey, Math.exp(scaleFactor * utility + scaleConstant));
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
            
            // from: 8721, to: 3932
            // from: 7250, to: 24565
            
            // 0.03
            // from: 12630, to: 18586 works, sometimes fails
            // from: 23556, to: 26547 numerical error, downUtil=0
            // from: 800, to: 22966 works, sometimes fails
            
            // 0.05
            // from: 4020, to: 14150 numerical error, downUtil=0, length parameter must be smaller
            // from: 17847, to: 10890 numerical error, downUtil=0, length parameter must be smaller
            int failedCounter = 0;
            for (int i=1; i < 11; i++) {
            	
              Random random = new Random();
              int randomIndex1 = random.nextInt(nodeIdList.size());
              Id<Node> destinationNodeId = nodeIdList.get(randomIndex1);
              int randomIndex2 = random.nextInt(nodeIdList.size());
              Id<Node> originNodeId = nodeIdList.get(randomIndex2);
              System.out.println("from: "+ originNodeId + ", to: " + destinationNodeId);
 
//              Id<Node> originNodeId = Id.createNodeId("17847");
//              Id<Node> destinationNodeId = Id.createNodeId("10890");
//              System.out.println("from: "+ originNodeId + ", to: " + destinationNodeId);
             
              long Time2 = System.currentTimeMillis();
              ArrayList<Id> routeLinkIds = predictRoute(originNodeId, destinationNodeId, network_milos, linkIdToIndexMap, matlab, matrixMapExp);
              if (routeLinkIds != null) {
                  saveRouteToCsv(routeLinkIds, originNodeId, destinationNodeId, network_milos);
                  long Time3 = System.currentTimeMillis(); 
                  long timeDiff2 = Time3 - Time2;
                  System.out.println("[i] Total time: " + timeDiff2 + " milliseconds");  
              }
              else {
            	  failedCounter += 1;
              }
            }
            
            System.out.println("number of failed routes: " + failedCounter);
			matlab.close();	
			System.out.println("MATLab engine closed");
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
        
	} 
	
	
	public static void saveRouteToCsv(ArrayList<Id> routeLinkIds, Id originNodeId, Id destinationNodeId, Network network_milos) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        String csvFilePath = "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\routes\\" + originNodeId + "_" + destinationNodeId + "_" + timestamp + ".csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath))) {
            writer.println("i,x,y");
            
        	for (int i = routeLinkIds.size() - 1; i >= 0; i--) {
                Id currentId = routeLinkIds.get(i);
                writer.println(i + "," + network_milos.getLinks().get(currentId).getToNode().getCoord().getX() + "," + network_milos.getLinks().get(currentId).getToNode().getCoord().getY());
        	}
            System.out.println("CSV file written successfully.");
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
	}
	
	public static ArrayList<Id> predictRoute(Id originNodeId, Id destinationNodeId, Network network_milos, Map<Id<Link>, Integer> linkIdToIndexMap, MatlabEngine matlab, Map<String, Double> matrixMapExp){

        
        StringBuilder rowIndicesString = new StringBuilder();
        for (Link l: network_milos.getNodes().get(destinationNodeId).getInLinks().values()) {
        	rowIndicesString.append(linkIdToIndexMap.get(l.getId())+1).append(","); // MATLab 1-based indexing
        }
        rowIndicesString.deleteCharAt(rowIndicesString.length() - 1);
        
        double[] downstreamUtility = null;


        try {
            matlab.eval("M_sp = sparse(rowIndices, colIndices, values);"); // exp of the systematic utility matrix
            matlab.eval("[n m]= size(M_sp);");
			matlab.eval("M_dest = M_sp(1:n,1:n);");
	        matlab.eval("M_dest(:,n+1) = 0;");
	        matlab.eval("M_dest([" + rowIndicesString.toString() + "], end) = 1;");
	        matlab.eval("M_dest(n+1,:) = zeros(1,n+1);");
	        matlab.eval("[n m]= size(M_dest);");
	        matlab.eval("b = zeros(n,1);");
	        matlab.eval("b(n)=1;");
	        matlab.eval("b_sp = sparse(b);");
	        matlab.eval("I = speye(size(M_dest));");
	        matlab.eval("A = I - M_dest;");
	        matlab.eval("Z = mldivide(A, b_sp);");
//	        matlab.eval("minele = min(Z);");
//	        matlab.eval("boolV0 = 1;");
//	        matlab.eval("if minele < -1e-3\n boolV0 = 0;\n end");   // NUM_ERROR = -1e-3;
	        matlab.eval("Zabs = full(abs(Z));");
//	        matlab.eval("resNorm = norm(A * Zabs - b_sp);");
//	        matlab.eval("if resNorm > 1e-3\n boolV0 = 0;\n end");   // RESIDUAL  =  1e-3; 
	        downstreamUtility = matlab.getVariable("Zabs");  
	        
		} catch (Exception e) {
			e.printStackTrace();
		}
             
        // loop through network starting with origin until destination reached
        ArrayList<Integer> routeNodeIds = new ArrayList<>();
        ArrayList<Id> routeLinkIds = new ArrayList<>();
        boolean isFirstIteration = true;
        Id nextLinkId = null;
        Id currentInNodeId = null;
        int currentInNodeInLinkIndex = 0;
        int iterationCounter = 0;
        while (true) {
        	if (isFirstIteration) {
        		currentInNodeId = originNodeId;
        		Map<Integer, Double> downstreamMapInitialLink = new HashMap<>();
        		
            	for (Link inLink : network_milos.getNodes().get(currentInNodeId).getInLinks().values()) {  
            		if (inLink.getAllowedModes().contains(TransportMode.bike)) {
            			currentInNodeInLinkIndex = linkIdToIndexMap.get(inLink.getId()); // needs to be bike link (simplify with bike-only network)
            			downstreamMapInitialLink.put(currentInNodeInLinkIndex, downstreamUtility[currentInNodeInLinkIndex]);
            		}
            	}
            	 Entry<Integer, Double> maxEntry = Collections.max(downstreamMapInitialLink.entrySet(), Map.Entry.comparingByValue()); // start with link that has highest downstream utility (to avoid taking the one with the lowest and hence not chosing most direkt link as first path link)
            	 currentInNodeInLinkIndex = maxEntry.getKey();
        		 isFirstIteration = false;
        	} else {
        		currentInNodeId = network_milos.getLinks().get(nextLinkId).getToNode().getId();
        		currentInNodeInLinkIndex = linkIdToIndexMap.get(nextLinkId);
        	}

        	double denominator = 0;
//        	ArrayList<Double> sysUtils = new ArrayList<>();
//        	ArrayList<Double> downstreamUtils = new ArrayList<>();
            for (Link l: network_milos.getNodes().get(currentInNodeId).getOutLinks().values()) {
            	if (l.getAllowedModes().contains(TransportMode.bike)) {
                	int indexOutLink = linkIdToIndexMap.get(l.getId());
                	String matrixKey = currentInNodeInLinkIndex + "," + indexOutLink;
                	double sysUtil = matrixMapExp.get(matrixKey);
                	double downstreamUtil = downstreamUtility[indexOutLink];
                	denominator = denominator + (downstreamUtil * sysUtil);
//                	sysUtils.add(sysUtil);
//                	downstreamUtils.add(downstreamUtil);
            	}
            }
            
//            double denominator_norm = 0;
//        	ArrayList<Double> sysUtils_norm = normalize(sysUtils);
//        	ArrayList<Double> downstreamUtils_norm =normalize(downstreamUtils);
//        	for (int j = 0; j < sysUtils.size(); j++) {
//        		denominator_norm = denominator_norm + (downstreamUtils_norm.get(j) * sysUtils_norm.get(j));
//        	}
 
            if (denominator == 0.0) {
            	System.out.println("Numerical problem, downstream Util = 0");
            	return null;
            }
            
            
            ArrayList<Double> downUtilValues = new ArrayList<>();
            ArrayList<Double> sysUtilValues = new ArrayList<>();
            ArrayList<Double> probValues = new ArrayList<>();
            ArrayList<Id> linkIds = new ArrayList<>();
            int runnningIndex = 0;
            for (Link l: network_milos.getNodes().get(currentInNodeId).getOutLinks().values()) {
            	if (l.getAllowedModes().contains(TransportMode.bike)) {
                	int indexOutLink = linkIdToIndexMap.get(l.getId());
                	String matrixKey = currentInNodeInLinkIndex + "," + indexOutLink;
                	
//                	double sysUtil = sysUtils_norm.get(runnningIndex);
//                	double downstreamUtil = downstreamUtils_norm.get(runnningIndex);
//                	double nominator = (downstreamUtil * sysUtil);
                	
                	double sysUtil = matrixMapExp.get(matrixKey);
                	double downstreamUtil = downstreamUtility[indexOutLink];
                	double nominator = (downstreamUtil * sysUtil);
                      	
                	System.out.println("link id " + l.getId() + ", SysUtil= " + sysUtil + ", DownUtil= " + downstreamUtil + ", No=" + nominator + ", Deno= " + denominator + ", P= " + (nominator/denominator));
                	probValues.add((nominator/denominator));
                	linkIds.add(l.getId());
                	downUtilValues.add(downstreamUtil);
                	sysUtilValues.add(sysUtil);
                	runnningIndex +=1; 
            	}
            }
            
//            nextLinkId = linkIds.get(findMaxDownUtilProbLink(sysUtilValues, downUtilValues));
//            nextLinkId = linkIds.get(sampleLink(probValues));
//            nextLinkId = linkIds.get(choseMaxProbLink(probValues));
//            nextLinkId = linkIds.get(choseMaxProbLink(downUtilValues));
            nextLinkId = sampleLinkNoDoubleEntries(probValues, routeLinkIds, linkIds);
            System.out.println("chosen link: " + nextLinkId); 
            routeLinkIds.add(nextLinkId);
            Id nextLinkToNodeId = network_milos.getLinks().get(nextLinkId).getToNode().getId();
            if (nextLinkToNodeId.equals(destinationNodeId)) {
            	System.out.println("destination found"); 
            	System.out.println("number of links: " + routeLinkIds.size()); 
            	break;
            }
            
            iterationCounter += 1;
            if (iterationCounter > 1000) {
            	System.out.println("infinite looping");
            	return null;
            }
            
        }

		return routeLinkIds;
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
       
    public static Id sampleLinkNoDoubleEntries(ArrayList<Double> probabilities, ArrayList<Id> previousLinkIds, ArrayList<Id> linkIds) {
        
    	
        if (linkIds.size() > 1) {
	    	for (int i = linkIds.size() - 1; i >= 0; i--) {
	            Id currentId = linkIds.get(i);
	         // todo: make sure not to create deadend: 
	         // if linkIds.size() == 2 && linkIds.contains(REVERSED last entry of previousLinkIds) {delete REVERSED entry}
	            if (previousLinkIds.contains(currentId) && linkIds.size() > 1) {
	                linkIds.remove(i);
	                probabilities.remove(i);
	                System.out.println("alternative removed: " + currentId);
	            }
	        }
        }
        
        double sumOfProbabilities = probabilities.stream().mapToDouble(Double::doubleValue).sum();
        if (sumOfProbabilities <= 0.0) {
            throw new IllegalArgumentException("Sum of probabilities must be greater than 0.");
        }
        for (int i = 0; i < probabilities.size(); i++) {
            probabilities.set(i, probabilities.get(i) / sumOfProbabilities);
        }
    	
        Random rand = new Random();
        double randomValue = rand.nextDouble(); // Generate a random number between 0 and 1
        double cumulativeProbability = 0.0;
        
//        System.out.println("size: " + probabilities.size());

        for (int i = 0; i < probabilities.size(); i++) {
            cumulativeProbability += probabilities.get(i);

            if (randomValue <= cumulativeProbability) {
                return linkIds.get(i); // Return the chosen alternative
            }
        }
        
        return null;
    }
    
    public static Id choseMaxProbLinkNoDoubleEntries(ArrayList<Double> probabilities, ArrayList<Id> previousLinkIds, ArrayList<Id> linkIds) {
        
        if (linkIds.size() > 1) {
	    	for (int i = linkIds.size() - 1; i >= 0; i--) {
	            Id currentId = linkIds.get(i);
	            if (previousLinkIds.contains(currentId) && linkIds.size() > 1) {
	                linkIds.remove(i);
	                probabilities.remove(i);
	                System.out.println("alternative removed");
	            }
	        }
        }
        
        double maxValue = probabilities.get(0);
        int maxIndex = 0;

        for (int i = 1; i < probabilities.size(); i++) {
            if (probabilities.get(i) > maxValue) {
                maxValue = probabilities.get(i);
                maxIndex = i;
            }
        }

        return linkIds.get(maxIndex);
    }
    
    public static boolean containsPattern(String input, String pattern) {
        // Compile the regular expression pattern
        Pattern regex = Pattern.compile(pattern);

        // Create a Matcher object
        Matcher matcher = regex.matcher(input);

        // Use Matcher's find() method to check if the pattern is found in the input string
        return matcher.find();
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
	
    public static ArrayList<Double> normalize(ArrayList<Double> values) {
        // Calculate the sum of all values
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        // Normalize each value by dividing by the sum
        ArrayList<Double> normalizedValues = new ArrayList<>();
        for (double value : values) {
            normalizedValues.add(value / sum);
        }

        return normalizedValues;
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
