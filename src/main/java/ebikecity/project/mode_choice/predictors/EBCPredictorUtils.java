package ebikecity.project.mode_choice.predictors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;


public class EBCPredictorUtils {
	static public double getHouseholdIncome(Person person) {
		return (Double) person.getAttributes().getAttribute("householdIncome");
	}

	static public boolean isAgeOver60(Person person) {
		return (int) (Integer) person.getAttributes().getAttribute("age") >= 60;
	}

	static public boolean hasPurposeWork(Activity activity) {
		return activity.getType().equals("work");
	}

	static public boolean isInsideCity(Activity activity) {
		Boolean isInside = (Boolean) activity.getAttributes().getAttribute("city");
		return isInside != null && isInside;
	}

	static public boolean isFemale(Person person) {
		return person.getAttributes().getAttribute("sex") == "f";
	}

	static public boolean isUrbaLevel2(Person person) {
		return person.getAttributes().getAttribute("urbanisation_level") == "medium";
	}

	static public boolean isUrbaLevel3(Person person) {
		return person.getAttributes().getAttribute("urbanisation_level") == "low";
	}

	static public int getAge(Person person){
		return (int) person.getAttributes().getAttribute("age");
	}
}
