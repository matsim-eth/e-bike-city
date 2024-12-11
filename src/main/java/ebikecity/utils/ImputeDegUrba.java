package ebikecity.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;


public class ImputeDegUrba {

    // adds degree of urbanization information to agents
	
    // args
    // [0] path to original MATSim population
    // [1] CSV data linking statpop ID with degree of urbanization
    // [2] path to output population
    // [3] path to csv output population
	
    public static Map<String, List<String>> loadCSV(String filePath) {
        Map<String, List<String>> idSpatialMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            int idIndex = -1;
            int urbanisationIndex = -1;
            int homeXIndex = -1;
            int homeYIndex = -1;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                
                if (isFirstLine) {
                    // Determine the indices of all columns of interest
                    for (int i = 0; i < values.length; i++) {
                        switch (values[i].trim().toLowerCase()) {
                            case "statpop_person_id":
                                idIndex = i;
                                break;
                            case "urbanisation_level":
                                urbanisationIndex = i;
                                break;
                            case "home_x":
                                homeXIndex = i;
                                break;
                            case "home_y":
                                homeYIndex = i;
                                break;
                    	}
                    }
                    isFirstLine = false;
                    
                    if (idIndex == -1 || urbanisationIndex == -1 || homeXIndex == -1 || homeYIndex == -1) {
                        throw new IOException("CSV file does not contain required columns: statpop_person_id, urbanisation_level, home_x, and home_y");
                    }
                } else {
                    // Add ID and SpatialAttribute to the map
                    String id = values[idIndex].trim();
                    List<String> attributes = new ArrayList<>();
                    
                    attributes.add(values[urbanisationIndex].trim());
                    attributes.add(values[homeXIndex]);
                    attributes.add(values[homeYIndex]);
                    
                    idSpatialMap.put(id, attributes);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return idSpatialMap;
    }
    
    private static void addSpatialAttributesToPopulation(Population population, Map<String, List<String>> idSpatialMap) {
    
        for (Person person : population.getPersons().values()) {
            String personId = person.getId().toString();
            
            // Skip persons whose ID starts with "freight"
            if (personId.toLowerCase().startsWith("freight")) {
                continue;
            }
            
            Attributes personAttributes = person.getAttributes();
            Object statpopPersonIdObj = personAttributes.getAttribute("statpopPersonId");
            if (statpopPersonIdObj != null) {
            	String statpopPersonId = statpopPersonIdObj.toString();
            	if (idSpatialMap.containsKey(statpopPersonId)){
            		List<String> attributes = idSpatialMap.get(statpopPersonId);
            		if (attributes.size() == 3){
            		    personAttributes.putAttribute("urbanisation_level", attributes.get(0));
            		    //personAttributes.putAttribute("home_x", attributes.get(1));
            		    //personAttributes.putAttribute("home_y", attributes.get(2));
            		} else {
                            System.err.println("Unexpected number of attributes for person " + statpopPersonId);
                        }
            	}
            }
            
        }
        
    }
    
    public static void exportPopulationToCSV(Population population, String filePath){
    
    	String[] IGNORED_ATTRIBUTES = new String[]{"vehicles"};
    	
    	List<String> ignoredAttributes = List.of(IGNORED_ATTRIBUTES);

	List<String> attributes = population.getPersons().values().stream()
                .flatMap(p -> p.getAttributes().getAsMap().keySet().stream())
                .distinct()
                .filter(attribute -> !ignoredAttributes.contains(attribute))
                .collect(Collectors.toList());
                
        String[] header = new String[attributes.size()+1];
        header[0] = "person_id";
        for(int i=0; i<attributes.size(); i++) {
            header[i+1] = attributes.get(i);
        }
        
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(String.join(";", header) + "\n");
            for(Person person: population.getPersons().values()) {
                String[] line = new String[attributes.size()+1];
                line[0] = person.getId().toString();
                for(int i=0; i<attributes.size(); i++) {
                    System.out.println(attributes.get(i) + "  " +String.valueOf(person.getAttributes().getAsMap().getOrDefault(attributes.get(i), null)) );
                    line[i+1] = String.valueOf(person.getAttributes().getAsMap().getOrDefault(attributes.get(i), null));
                }
                fileWriter.write(String.join(";", line) + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    
    
    }
    
	
    public static void main(String[] args) throws IOException {
		
	Config config = ConfigUtils.createConfig();
		
	Scenario scenario = ScenarioUtils.createMutableScenario(config);

	PopulationReader popReader = new PopulationReader(scenario);
	popReader.readFile(args[0]);
	
	Population population = scenario.getPopulation();
		
	Map<String, List<String>> idSpatialMap = loadCSV(args[1]);
	addSpatialAttributesToPopulation(population, idSpatialMap);	
		
	new PopulationWriter(population).write(args[2]);
	
	//exportPopulationToCSV(population, args[3]);
		
	}

}
