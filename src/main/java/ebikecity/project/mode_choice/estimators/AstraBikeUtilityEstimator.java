package ebikecity.project.mode_choice.estimators;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.AstraModeParameters;
import ebikecity.project.mode_choice.predictors.AccessEgressBikePredictor;
import ebikecity.project.mode_choice.predictors.AstraBikePredictor;
// import ebikecity.project.mode_choice.predictors.AstraBikePredictor;
import ebikecity.project.mode_choice.predictors.AstraPersonPredictor;
import ebikecity.project.mode_choice.predictors.AstraTripPredictor;
import ebikecity.project.mode_choice.variables.AstraBikeVariables;
import ebikecity.project.mode_choice.variables.AstraPersonVariables;
import ebikecity.project.mode_choice.variables.AstraTripVariables;

public class AstraBikeUtilityEstimator extends SwissBikeUtilityEstimator {
	static public final String NAME = "AstraBikeEstimator";

	private final AstraModeParameters parameters;
	// private finel AccessEgressBikePredictor predictor;
	private final AstraBikePredictor predictor;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;

	@Inject
	// public AstraBikeUtilityEstimator(AstraModeParameters parameters, AccessEgressBikePredictor predictor,
	public AstraBikeUtilityEstimator(AstraModeParameters parameters, AstraBikePredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		
		super(parameters, personPredictor.delegate, predictor.delegate);
		// super(parameters, personPredictor.delegate, predictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(AstraBikeVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraBike.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraBike.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AstraBikeVariables variables = (AstraBikeVariables) predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		
		// List that stores information to be mapped onto trip
		List<String> store = new ArrayList<String>();
		store.add(person.getId().toString());
		store.add(person.getId().toString() + "_" + Integer.toString(trip.getIndex()+1));
		store.add(Double.toString(trip.getDepartureTime()));
		store.add(trip.getOriginActivity().getFacilityId().toString());
		store.add("bike");
		store.add(Double.toString(variables.travelTime_min * 60));
		store.add(Double.toString(utility));
				
		UtilityContainer container = UtilityContainer.getInstance();
		container.getUtilites().add(store);

		return utility;
	}
}
