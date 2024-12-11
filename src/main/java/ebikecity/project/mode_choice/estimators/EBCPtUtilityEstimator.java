package ebikecity.project.mode_choice.estimators;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.EBCModeParameters;
import ebikecity.project.mode_choice.predictors.EBCPersonPredictor;
import ebikecity.project.mode_choice.predictors.EBCPtPredictor;
import ebikecity.project.mode_choice.predictors.EBCTripPredictor;
import ebikecity.project.mode_choice.variables.EBCPersonVariables;
import ebikecity.project.mode_choice.variables.EBCPtVariables;
import ebikecity.project.mode_choice.variables.EBCTripVariables;

public class EBCPtUtilityEstimator extends PtUtilityEstimator {
	static public final String NAME = "EBCPtEstimator";

	private final EBCModeParameters parameters;
	private final EBCPtPredictor predictor;
	private final EBCPersonPredictor personPredictor;
	private final EBCTripPredictor tripPredictor;

	@Inject
	public EBCPtUtilityEstimator(EBCModeParameters parameters, EBCPtPredictor predictor,
			EBCPersonPredictor personPredictor, EBCTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateAgeUtility(EBCPersonVariables variables) {
		return this.parameters.ebcWalk.age * variables.age;
	}

	protected double estimateFemaleUtility(EBCPersonVariables variables){
		int isfemale = variables.isFemale ? 1 : 0;
		return this.parameters.ebcPt.female * isfemale;
	}

	protected double estimateUrbaLevel2Utility(EBCPersonVariables variables){
		int urblevel2 = variables.isUrbaLevel2 ? 1 : 0;
		return this.parameters.ebcPt.urbLevel2 * urblevel2;
	}

	protected double estimateUrbaLevel3Utility(EBCPersonVariables variables){
		int urblevel3 = variables.isUrbaLevel3 ? 1 : 0;
		return this.parameters.ebcPt.urbLevel3 * urblevel3;
	}

	protected double estimateExternalitiesUtility(EBCTripVariables variables){
		return this.parameters.ebcPt.beta_externalities * this.parameters.ebcPt.ext_by_km * variables.networkDistance_km;
	}

	protected double estimateInVehicleTimeUtility(EBCPtVariables variables) {
		double inVehicleTime = variables.railTravelTime_min + variables.busTravelTime_min;
		return parameters.ebcPt.betaTravelTime_min * inVehicleTime / 60.0;
	}

	protected double estimateMonetaryCostUtility(EBCPtVariables variables, EBCPersonVariables personVariables) {
		boolean has_abo = personVariables.hasGeneralSubscription || personVariables.hasHalbtaxSubscription || personVariables.hasRegionalSubscription ;
		double networkDistance_km = variables.inVehicleDistance_km;
		double factor_abo = has_abo ? 0.5 : 1.0;
		double cost_distance = 0.0;
		if (networkDistance_km <= 5){
			cost_distance = 0.89 * networkDistance_km;
		}
		else{
			cost_distance = 0.589 * networkDistance_km;
		}
		if (cost_distance < 3.4){
			cost_distance = 3.4;
		}		
		cost_distance = cost_distance * factor_abo;
		return this.parameters.ebcPt.beta_cost * cost_distance;
	}

	protected double estimateHeadwayUtility(EBCPtVariables variables) {
		if (parameters.ebcPt.betaHeadway_min != 0.0 && variables.headway_min == 0.0) {
			throw new IllegalStateException("Non-zero beta for headway, but no headway is given.");
		}

		return parameters.ebcPt.betaHeadway_min * variables.headway_min / 60.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		EBCPtVariables variables = predictor.predictVariables(person, trip, elements);
		EBCPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		EBCTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateFemaleUtility(personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateUrbaLevel2Utility(personVariables);
		utility += estimateUrbaLevel3Utility(personVariables);
		utility += estimateAccessEgressTimeUtility(variables) / 60.0;
		utility += estimateInVehicleTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateHeadwayUtility(variables);
		utility += estimateExternalitiesUtility(tripVariables);
		return utility;
	}
}


