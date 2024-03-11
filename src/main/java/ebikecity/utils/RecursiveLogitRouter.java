package ebikecity.utils;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.sparse.csc.CommonOps_DSCC;
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
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.MatrixR064;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.ojalgo.OjAlgoUtils;
import org.ojalgo.RecoverableCondition;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.decomposition.QR;
import org.ojalgo.matrix.store.ElementsSupplier;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.matrix.task.InverterTask;
import org.ojalgo.matrix.task.SolverTask;
import org.ojalgo.netio.BasicLogger;
import static org.ojalgo.type.CalendarDateUnit.*;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Random;
import org.ojalgo.OjAlgoUtils;
import org.ojalgo.array.ArrayR064;
import org.ojalgo.array.LongToNumberMap;
import org.ojalgo.array.SparseArray;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.Primitive64Store;
import org.ojalgo.matrix.store.SparseStore;
import org.ojalgo.matrix.task.iterative.ConjugateGradientSolver;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.series.BasicSeries;
import org.ojalgo.type.Stopwatch;



public class RecursiveLogitRouter {
	
	private Network network; 
	
	
	public RecursiveLogitRouter (Network network_milos) {
		// constructor 
		
		this.network = network_milos;	
		
        double paraLength = -0.07;
        double paraBikePath = 0.05;
		
		// generate hashmap for link indexes
		int numberOfLinks = 0;
        Map<Id<Link>, Integer> linkIdToIndexMap = new HashMap<>();
        
        for (Link link : network_milos.getLinks().values()) {
        	if (link.getAllowedModes().contains(TransportMode.bike)) {
                Id<Link> linkId = link.getId();
                if (!linkIdToIndexMap.containsKey(linkId)) {
                    linkIdToIndexMap.put(linkId, numberOfLinks++);
                }
        	}
        }   
       
        saveMapToCSV(linkIdToIndexMap, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\hashmap.csv");
        
        
        double[] values = {1.0, 2.0, 3.0, 4.0};
        int[] rowIndices = {0, 2, 1, 2};
        int[] colIndices = {0, 1, 2, 2};
        ArrayList<Double> valueList = new ArrayList<>();
        ArrayList<Integer> rowIndicesList = new ArrayList<>();
        ArrayList<Integer> colIndicesList = new ArrayList<>();
        

        // Step 2: Instantiate DMatrixSparseCSC
//        int numRows = 3;
//        int numCols = 3;
//        DMatrixSparseCSC dmat = new DMatrixSparseCSC(numRows, numCols, values.length);
//        DMatrixSparseCSC dmat = new DMatrixSparseCSC(numRows, numCols, 0);
//        DMatrixSparseCSC dmat = new DMatrixSparseCSC(numberOfLinks, numberOfLinks, 0);

        // Step 3: Set Values and Indices
//        for (int i = 0; i < values.length; i++) {
//            dmat.set(rowIndices[i], colIndices[i], values[i]);
//        }
               
        
        // construct connectivity matrix 
//        int[][] connectivityMatrix = new int[numberOfLinks][numberOfLinks];
//        double[][] sysUtilityMatrix = new double[numberOfLinks][numberOfLinks];
        for (Link link : network_milos.getLinks().values()) {
        	if (link.getAllowedModes().contains(TransportMode.bike)) {
	            Id<Link> linkId = link.getId();
	            Set<Id<Link>> outgoingLinks = link.getToNode().getOutLinks().keySet();
	            for (Id<Link> outgoingLinkId : outgoingLinks) {
	            	
	            	if (network_milos.getLinks().get(outgoingLinkId).getAllowedModes().contains(TransportMode.bike)) {
	            	
		                int outgoingLinkIndex = linkIdToIndexMap.get(outgoingLinkId);
		                int currentLinkIndex = linkIdToIndexMap.get(linkId);
		//                connectivityMatrix[currentLinkIndex][outgoingLinkIndex] = 1; 
		//                dmat.set(currentLinkIndex, outgoingLinkIndex, 1);
		//                valueList.add(1.0);
		                rowIndicesList.add(currentLinkIndex);
		                colIndicesList.add(outgoingLinkIndex);
		                double utility = network_milos.getLinks().get(outgoingLinkId).getLength() * paraLength;
		                if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes") != null) {
		                    if (network_milos.getLinks().get(outgoingLinkId).getAttributes().getAttribute("osm:way:lanes").toString().indexOf("P") != -1) {
		                        System.out.println("found");
		                        utility = utility + network_milos.getLinks().get(outgoingLinkId).getLength() * paraBikePath;
		                    }
		                	
		                }
		
		//                sysUtilityMatrix[currentLinkIndex][outgoingLinkIndex] = utility;
		//                dmat.set(currentLinkIndex, outgoingLinkIndex, utility);
		                valueList.add(utility);
	            		}
	                }
        		}
                
            }
        
        // assuming only one link connecting to the destination link 
        valueList.add(1.0);
        System.out.println("max row index: " + Collections.max(rowIndicesList));
        System.out.println("number of links: " + numberOfLinks);
        rowIndicesList.add(numberOfLinks);
        colIndicesList.add(100);   // define which links connects to destination 
        numberOfLinks++;
        System.out.println("Time A: " + LocalTime.now());
        
        
        DMatrixSparseCSC dmat = new DMatrixSparseCSC(numberOfLinks, numberOfLinks, 0);
        for (int i = 0; i < valueList.size(); i++) {
            dmat.set(rowIndicesList.get(i), colIndicesList.get(i), valueList.get(i));
        }    
        System.out.println("Time B: " + LocalTime.now());
        
        DMatrixSparseCSC identityMatrix = CommonOps_DSCC.identity(numberOfLinks);
        System.out.println("Time C: " + LocalTime.now());
        
        DMatrixSparseCSC resultdmat = new DMatrixSparseCSC(numberOfLinks, numberOfLinks, 0);
        System.out.println("Time D: " + LocalTime.now());
        
        CommonOps_DSCC.add(1.0, identityMatrix, -1.0, dmat, resultdmat, null, null);
        System.out.println("Time E: " + LocalTime.now());
        
        DMatrixSparseCSC b = new DMatrixSparseCSC(numberOfLinks, 1, 1);
        int startLinkIndex = 0;
        b.set(startLinkIndex, 0, 1);
        System.out.println("Time F: " + LocalTime.now());
        
        DMatrixSparseCSC x = new DMatrixSparseCSC(numberOfLinks, 1);
        System.out.println("Time G: " + LocalTime.now());
        
        CommonOps_DSCC.solve(resultdmat, b, x);
        System.out.println("Time H: " + LocalTime.now());
                
        
        // Print DMatrix information
        System.out.println("Number of Rows: " + x.getNumRows());
        System.out.println("Number of Columns: " + x.getNumCols());
        System.out.println("Number of Non-Zero Elements: " + x.nz_length);
        System.out.println("Values: " + java.util.Arrays.toString(x.nz_values));
        System.out.println("Row Indices: " + java.util.Arrays.toString(x.nz_rows));
        
        
        
//	    saveIntMatrixToCSV(connectivityMatrix, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\connectivity_matrix.csv", linkIdToIndexMap);
//        saveIntMatrixToCSV(sysUtilityMatrix, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\systematic_utility_matrix.csv", linkIdToIndexMap);

        
        // construct sparse connectivity matrix 
//        Map<Integer, Map<Integer, Double>> sparseConnectivityMatrix = new HashMap<>();
//
//        for (Link link : network_milos.getLinks().values()) {
//            Id<Link> linkId = link.getId();
//            Set<Id<Link>> outgoingLinks = link.getToNode().getOutLinks().keySet();
//
//            for (Id<Link> outgoingLinkId : outgoingLinks) {
//                int outgoingLinkIndex = linkIdToIndexMap.get(outgoingLinkId);
//                int currentLinkIndex = linkIdToIndexMap.get(linkId);
//
//                // Check if the row exists in the sparse matrix
//                if (!sparseConnectivityMatrix.containsKey(currentLinkIndex)) {
//                    sparseConnectivityMatrix.put(currentLinkIndex, new HashMap<>());
//                }
//
//                // Set the non-zero entry in the sparse matrix
////                sparseConnectivityMatrix.get(currentLinkIndex).put(outgoingLinkIndex, 1);
//                double utility = network_milos.getLinks().get(outgoingLinkId).getLength() * paraLength;
//                sparseConnectivityMatrix.get(currentLinkIndex).put(outgoingLinkIndex, utility);
//            }
//        }
//        saveSparseDoubleMatrixToCSV(sparseConnectivityMatrix, "C:\\Users\\admeister\\Desktop\\ethz\\projects\\route_choice\\EBIS\\recursive_logit\\connectivity_matrix_sparse.csv");    
        
 
        
//        // generate identity matrix 
//        Map<Integer, Map<Integer, Double>> identityMatrix = new HashMap<>();
//        for (int i = 0; i < numberOfLinks; i++) {
//            identityMatrix.put(i, new HashMap<>());
//            identityMatrix.get(i).put(i, 1.0);
//        }
//        
//        // perform subtraction from identity matrix
//        Map<Integer, Map<Integer, Double>> resultMatrix = new HashMap<>(identityMatrix);
//
//        for (int i : sparseConnectivityMatrix.keySet()) {
//            if (!resultMatrix.containsKey(i)) {
//                resultMatrix.put(i, new HashMap<>());
//            }
//
//            for (int j : sparseConnectivityMatrix.get(i).keySet()) {
//                double value = sparseConnectivityMatrix.get(i).get(j);
//
//                // Subtract the value from the corresponding position in the identity matrix
//                resultMatrix.get(i).put(j, resultMatrix.get(i).getOrDefault(j, 0.0) - value);
//            }
//        }
        
	}


    public static void saveSparseDoubleMatrixToCSV(Map<Integer, Map<Integer, Double>> sparseMatrix, String filePath) {
        try (FileWriter csvWriter = new FileWriter(filePath)) {
            for (Map.Entry<Integer, Map<Integer, Double>> entry : sparseMatrix.entrySet()) {
                int row = entry.getKey();
                Map<Integer, Double> rowData = entry.getValue();

                for (Map.Entry<Integer, Double> colEntry : rowData.entrySet()) {
                    int col = colEntry.getKey();
                    double value = colEntry.getValue();

                    // Write row, column, and value to CSV
                    csvWriter.append(String.valueOf(row)).append(",");
                    csvWriter.append(String.valueOf(col)).append(",");
                    csvWriter.append(String.valueOf(value)).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    
    public static void saveSparseIntMatrixToCSV(Map<Integer, Map<Integer, Integer>> sparseMatrix, String filePath) {
        try (FileWriter csvWriter = new FileWriter(filePath)) {
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : sparseMatrix.entrySet()) {
                int row = entry.getKey();
                Map<Integer, Integer> rowData = entry.getValue();

                for (Map.Entry<Integer, Integer> colEntry : rowData.entrySet()) {
                    int col = colEntry.getKey();
                    int value = colEntry.getValue();

                    // Write row, column, and value to CSV
                    csvWriter.append(String.valueOf(row)).append(",");
                    csvWriter.append(String.valueOf(col)).append(",");
                    csvWriter.append(String.valueOf(value)).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
		
	static void saveIntMatrixToCSV(int[][] matrix, String fileName, Map<Id<Link>, Integer> columnLabels) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write column labels
            writer.append("Links,");
            for (Map.Entry<Id<Link>, Integer> entry : columnLabels.entrySet()) {
                writer.append(entry.getKey().toString()).append(',');
            }
            writer.append('\n');

            // Write data
            for (Map.Entry<Id<Link>, Integer> rowEntry : columnLabels.entrySet()) {
                writer.append(rowEntry.getKey().toString()).append(',');
                for (Map.Entry<Id<Link>, Integer> colEntry : columnLabels.entrySet()) {
                    int rowIndex = rowEntry.getValue();
                    int colIndex = colEntry.getValue();
                    writer.append(String.valueOf(matrix[rowIndex][colIndex])).append(',');
                }
                writer.append('\n');
            }

            System.out.println("CSV file has been created successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	
	static void saveIntMatrixToCSV(double[][] matrix, String fileName, Map<Id<Link>, Integer> columnLabels) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write column labels
            writer.append("Links,");
            for (Map.Entry<Id<Link>, Integer> entry : columnLabels.entrySet()) {
                writer.append(entry.getKey().toString()).append(',');
            }
            writer.append('\n');

            // Write data
            for (Map.Entry<Id<Link>, Integer> rowEntry : columnLabels.entrySet()) {
                writer.append(rowEntry.getKey().toString()).append(',');
                for (Map.Entry<Id<Link>, Integer> colEntry : columnLabels.entrySet()) {
                    int rowIndex = rowEntry.getValue();
                    int colIndex = colEntry.getValue();
                    writer.append(String.valueOf(matrix[rowIndex][colIndex])).append(',');
                }
                writer.append('\n');
            }

            System.out.println("CSV file has been created successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
		
	private static void saveMapToCSV(Map<Id<Link>, Integer> map, String filePath) {
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

	
	public static void main (String[] args) {
		// main method
		
		Config config = ConfigUtils.createConfig(); 
		
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork()); 
		
		reader.readFile(args[0]);
		
		RecursiveLogitRouter my_new_router = new RecursiveLogitRouter(scenario.getNetwork());
		
		//predictRoute(network);
		
		scenario.getNetwork().getLinks().values().toArray();
		

	}
	
	

}
