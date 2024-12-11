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

import ebikecity.project.mode_choice.EBCModeParameters;
import ebikecity.project.mode_choice.predictors.AccessEgressCarPredictor;
import ebikecity.project.mode_choice.predictors.EBCPersonPredictor;
import ebikecity.project.mode_choice.predictors.EBCTripPredictor;
import ebikecity.project.mode_choice.variables.EBCPersonVariables;
import ebikecity.project.mode_choice.variables.EBCTripVariables;

public class EBCCarUtilityEstimator extends CarUtilityEstimator {
	static public final String NAME = "EBCCarEstimator";

	private final EBCModeParameters parameters;
	private final EBCPersonPredictor personPredictor;
	private final EBCTripPredictor tripPredictor;
	private final AccessEgressCarPredictor predictor;

	@Inject
	public EBCCarUtilityEstimator(EBCModeParameters parameters, AccessEgressCarPredictor predictor,
			EBCPersonPredictor personPredictor, EBCTripPredictor tripPredictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
		this.predictor = predictor;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return super.estimateTravelTimeUtility(variables) / 60.0 ;
	}

	protected double estimateMonetaryCostUtility(CarVariables variables, EBCPersonVariables personVariables, EBCTripVariables tripVariables) {
		double cost_by_km = 0.188;
		return this.parameters.ebcCar.beta_cost * cost_by_km * tripVariables.networkDistance_km;
	}

	protected double estimateExternalitiesUtility(EBCTripVariables variables){
		return this.parameters.ebcCar.beta_externalities * this.parameters.ebcCar.ext_by_km * variables.networkDistance_km;
	}

	protected double estimateParkingUtility(EBCTripVariables variables){
		double parkingCost = 0.0;
		if (!variables.isHome  && !variables.isWork){
			if (variables.isCity){
				parkingCost = 4 * variables.parkingDuration;
			}
			else{
				parkingCost = 2 * variables.parkingDuration;
			}
		}
		return this.parameters.ebcCar.beta_parking_cost * parkingCost;
	}


	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);
		EBCPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		EBCTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables, tripVariables);
		utility += estimateExternalitiesUtility(tripVariables);
		utility += estimateParkingUtility(tripVariables);

		Leg leg = (Leg) elements.get(0);
		leg.getAttributes().putAttribute("isNew", true);

		return utility;
	}
}
