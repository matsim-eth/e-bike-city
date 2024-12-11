package ebikecity.project.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
// import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.variables.AstraBikeVariables;


public class AccessEgressBikePredictor extends  BikePredictor {
// public class AccessEgressBikePredictor extends  CachedVariablePredictor<AstraBikeVariables> {

//	@Inject
//	public AccessEgressBikePredictor() {
//		super();
//	}
//
//	@Override
//	public AstraBikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
//		
//
//		Leg leg = (Leg) elements.get(2);
//
//		double travelTime_min = leg.getTravelTime().seconds() / 60.0 ;
//		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
//
//		return new AstraBikeVariables(new BikeVariables(travelTime_min), euclideanDistance_km);
//	}
	

	@Inject
	public void AcessEgressBikePredictor() {
	}

	@Override
	public AstraBikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		Leg leg = (Leg) elements.get(2);
	    double travelTime_min = leg.getTravelTime().seconds() / 60.0 ;
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new AstraBikeVariables(new BikeVariables(travelTime_min), euclideanDistance_km);
	}
}
