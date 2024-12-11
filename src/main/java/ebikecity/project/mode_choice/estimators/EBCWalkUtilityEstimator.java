package ebikecity.project.mode_choice.estimators;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.EBCModeParameters;
import ebikecity.project.mode_choice.predictors.EBCPersonPredictor;
import ebikecity.project.mode_choice.predictors.EBCTripPredictor;
import ebikecity.project.mode_choice.predictors.EBCWalkPredictor;
import ebikecity.project.mode_choice.variables.EBCPersonVariables;
import ebikecity.project.mode_choice.variables.EBCWalkVariables;
import ebikecity.project.mode_choice.variables.EBCTripVariables;

public class EBCWalkUtilityEstimator extends WalkUtilityEstimator {
	static public final String NAME = "EBCWalkEstimator";

	private final EBCModeParameters parameters;
	private final EBCWalkPredictor predictor;
	private final EBCPersonPredictor personPredictor;
	private final EBCTripPredictor tripPredictor;

	@Inject
	public EBCWalkUtilityEstimator(EBCModeParameters parameters, EBCWalkPredictor predictor,
			EBCPersonPredictor personPredictor, EBCTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(EBCWalkVariables variables) {
		return super.estimateTravelTimeUtility(variables) / 60.0;
	}

	protected double estimateAgeUtility(EBCPersonVariables variables) {
		return this.parameters.ebcWalk.age * variables.age;
	}

	protected double estimateFemaleUtility(EBCPersonVariables variables){
		int isfemale = variables.isFemale ? 1 : 0;
		return this.parameters.ebcWalk.female * isfemale;
	}

	protected double estimateUrbaLevel2Utility(EBCPersonVariables variables){
		int urblevel2 = variables.isUrbaLevel2 ? 1 : 0;
		return this.parameters.ebcWalk.urbLevel2 * urblevel2;
	}

	protected double estimateUrbaLevel3Utility(EBCPersonVariables variables){
		int urblevel3 = variables.isUrbaLevel3 ? 1 : 0;
		return this.parameters.ebcWalk.urbLevel3 * urblevel3;
	}

	protected double estimateExternalitiesUtility(EBCTripVariables variables){
		return this.parameters.ebcWalk.beta_externalities * this.parameters.ebcWalk.ext_by_km * variables.networkDistance_km;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		EBCWalkVariables variables = predictor.predictVariables(person, trip, elements);
		EBCPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		EBCTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

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
