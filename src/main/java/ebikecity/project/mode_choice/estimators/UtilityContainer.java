package ebikecity.project.mode_choice.estimators;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class UtilityContainer {
	private static final UtilityContainer INSTANCE = new UtilityContainer();
	private List<List<String>> utilites;
	
	public UtilityContainer() {
		this.utilites = new CopyOnWriteArrayList<List<String>>();
		
	}
	
	public static UtilityContainer getInstance() {
		return INSTANCE;
	}

	public List<List<String>> getUtilites() {
		return utilites;
	}

}
