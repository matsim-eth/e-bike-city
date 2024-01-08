package ebikecity.project.mode_choice.estimators;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.AstraModeParameters;
import ebikecity.project.mode_choice.EBikeModeParameters;
import ebikecity.project.mode_choice.predictors.AccessEgressCarPredictor;
import ebikecity.project.mode_choice.predictors.AstraPersonPredictor;
import ebikecity.project.mode_choice.predictors.AstraTripPredictor;
import ebikecity.project.mode_choice.variables.AstraPersonVariables;
import ebikecity.project.mode_choice.variables.AstraTripVariables;

public class EBikeCarUtilityEstimator extends CarUtilityEstimator {
	static public final String NAME = "EBikeCarEstimator";

	private final EBikeModeParameters parameters;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;
	private final AccessEgressCarPredictor predictor;
	// private final CarPredictor predictor;

	@Inject
	public EBikeCarUtilityEstimator(EBikeModeParameters parameters, AccessEgressCarPredictor predictor,
//	public AstraCarUtilityEstimator(AstraModeParameters parameters, CarPredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
		this.predictor = predictor;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateMonetaryCostUtility(CarVariables variables, AstraPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraCar.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraCar.betaWork : 0.0;
	}

	protected double estimateCityUtility(AstraTripVariables variables) {
		return variables.isCity ? parameters.astraCar.betaCity : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimateCityUtility(tripVariables);

		Leg leg = (Leg) elements.get(0);
		leg.getAttributes().putAttribute("isNew", true);
		
		// List that stores information to be mapped onto trip
		List<String> store = new ArrayList<String>();
		store.add(person.getId().toString());
		store.add(person.getId().toString() + "_" + Integer.toString(trip.getIndex()+1)); 
		store.add(Double.toString(trip.getDepartureTime()));
		store.add(trip.getOriginActivity().getFacilityId().toString());
		store.add("car");
		store.add(Double.toString(leg.getTravelTime().seconds()));
		store.add(Double.toString(utility));
		
		// How can I make all the estimators add their store into the same container??
		UtilityContainer container = UtilityContainer.getInstance();
		container.getUtilites().add(store);

		return utility;
	}
}