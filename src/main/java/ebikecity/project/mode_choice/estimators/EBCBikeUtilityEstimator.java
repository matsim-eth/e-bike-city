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

import ebikecity.project.mode_choice.EBCModeParameters;
import ebikecity.project.mode_choice.predictors.EBCAccessEgressBikePredictor;
import ebikecity.project.mode_choice.predictors.EBCBikePredictor;
import ebikecity.project.mode_choice.variables.EBCBikeVariables;
import ebikecity.project.mode_choice.predictors.EBCPersonPredictor;
import ebikecity.project.mode_choice.predictors.EBCTripPredictor;
import ebikecity.project.mode_choice.variables.EBCPersonVariables;
import ebikecity.project.mode_choice.variables.EBCTripVariables;

public class EBCBikeUtilityEstimator extends SwissBikeUtilityEstimator {
	static public final String NAME = "EBCBikeEstimator";

	private final EBCModeParameters parameters;
	private final EBCAccessEgressBikePredictor predictor;
	private final EBCPersonPredictor personPredictor;
	private final EBCTripPredictor tripPredictor;

	@Inject
	public EBCBikeUtilityEstimator(EBCModeParameters parameters, EBCAccessEgressBikePredictor predictor,
			EBCPersonPredictor personPredictor, EBCTripPredictor tripPredictor) {
		
		super(parameters, personPredictor.delegate, predictor);
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(EBCBikeVariables variables) {
		return super.estimateTravelTimeUtility(variables) / 60.0 ; // because the travel time considered in the utility function is in hours and the returned travel time is in min
	}

	protected double estimateAgeUtility(EBCPersonVariables variables) {
		return this.parameters.ebcBike.age * variables.age;
	}

	protected double estimateFemaleUtility(EBCPersonVariables variables){
		int isfemale = variables.isFemale ? 1 : 0;
		return this.parameters.ebcBike.female * isfemale;
	}

	protected double estimateUrbaLevel2Utility(EBCPersonVariables variables){
		int urblevel2 = variables.isUrbaLevel2 ? 1 : 0;
		return this.parameters.ebcBike.urbLevel2 * urblevel2;
	}

	protected double estimateUrbaLevel3Utility(EBCPersonVariables variables){
		int urblevel3 = variables.isUrbaLevel3 ? 1 : 0;
		return this.parameters.ebcBike.urbLevel3 * urblevel3;
	}

	protected double estimateExternalitiesUtility(EBCTripVariables variables){
		return this.parameters.ebcBike.beta_externalities * this.parameters.ebcBike.ext_by_km * variables.networkDistance_km;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		EBCBikeVariables variables       = (EBCBikeVariables) predictor.predictVariables(person, trip, elements);
		EBCPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		EBCTripVariables tripVariables     = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateFemaleUtility(personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateUrbaLevel2Utility(personVariables);
		utility += estimateUrbaLevel3Utility(personVariables);
		utility += estimateTravelTimeUtility(variables);
		utility += estimateExternalitiesUtility(tripVariables);
		return utility;
	}
}
