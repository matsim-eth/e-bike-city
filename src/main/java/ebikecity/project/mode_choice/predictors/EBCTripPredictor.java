package ebikecity.project.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.Time;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import ebikecity.project.mode_choice.variables.EBCTripVariables;

public class EBCTripPredictor extends CachedVariablePredictor<EBCTripVariables> {
	@Override
	protected EBCTripVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean isWork = AstraPredictorUtils.hasPurposeWork(trip.getDestinationActivity());
				//|| AstraPredictorUtils.hasPurposeWork(trip.getOriginActivity());

		boolean isHome = AstraPredictorUtils.hasPurposeHome(trip.getDestinationActivity());

		boolean isCity = AstraPredictorUtils.isInsideCity(trip.getDestinationActivity());
				//|| AstraPredictorUtils.isInsideCity(trip.getOriginActivity());


		double distance = 0.0;

		for (PlanElement pe : elements){
			if (pe instanceof Leg){
				Leg leg = (Leg)pe;
				distance += leg.getRoute().getDistance();
			}
		}
		/**Leg leg1 = (Leg) elements.get(2);
		distance = leg1.getRoute().getDistance();**/
		double distance_km = distance / 1e3;

		double parkingDuration = 0.0;

		if (elements.size() >= 3){
			// Access parking duration information
			Leg leg = (Leg) elements.get(2);
			// arrival time at parking
			double arrivalTime = (leg.getDepartureTime().seconds() + leg.getTravelTime().seconds()); 
			// departure time from parking: ~ end time of destination activity
			double departureTime = AstraPredictorUtils.calculateEndTimeInSeconds(trip.getDestinationActivity());
			if (Double.isNaN(departureTime)) {
				departureTime = 30 * 3600;
			}
			// parking duration in hours
			parkingDuration = (departureTime - arrivalTime) / 3600.0;
		}

		return new EBCTripVariables(isWork, isHome, isCity, distance_km, parkingDuration);
	}
}
