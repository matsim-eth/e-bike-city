package ebikecity.project.mode_choice.predictors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

public class AstraPredictorUtils {
	static public double getHouseholdIncome(Person person) {
		return (Double) person.getAttributes().getAttribute("householdIncome");
	}

	static public boolean isAgeOver60(Person person) {
		return (int) (Integer) person.getAttributes().getAttribute("age") >= 60;
	}

	static public boolean hasPurposeWork(Activity activity) {
		return activity.getType().equals("work");
	}

	static public boolean hasPurposeHome(Activity activity) {
		return activity.getType().equals("home");
	}

	public static double calculateEndTimeInSeconds(Activity activity) {
        double endTimeInSeconds = Double.NaN;

        // Check if end time is explicitly set
        if (activity.getEndTime().isDefined()) {
            endTimeInSeconds = activity.getEndTime().seconds();
        } 
        // If end time is not set, calculate based on start time and duration
        else if (activity.getStartTime().isDefined() && activity.getMaximumDuration().isDefined()) {
            double startTimeInSeconds = activity.getStartTime().seconds();
            double durationInSeconds = activity.getMaximumDuration().seconds();
            endTimeInSeconds = startTimeInSeconds + durationInSeconds;
        }
        // If neither end time nor start time and duration are set, return NaN
        else {
            //System.out.println("Warning: Unable to calculate end time for activity " + activity.getType());
            return 30.0*3600;
        }

        // Ensure end time is not negative
        return Math.max(0, endTimeInSeconds);
    }

	static public boolean isInsideCity(Activity activity) {
		Boolean isInside = (Boolean) activity.getAttributes().getAttribute("city");
		return isInside != null && isInside;
	}
}
