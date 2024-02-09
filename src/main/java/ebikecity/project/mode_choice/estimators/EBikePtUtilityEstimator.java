package ebikecity.project.mode_choice.estimators;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.AstraModeParameters;
import ebikecity.project.mode_choice.EBikeModeParameters;
import ebikecity.project.mode_choice.predictors.AstraPersonPredictor;
import ebikecity.project.mode_choice.predictors.AstraPtPredictor;
import ebikecity.project.mode_choice.predictors.AstraTripPredictor;
import ebikecity.project.mode_choice.variables.AstraPersonVariables;
import ebikecity.project.mode_choice.variables.AstraPtVariables;
import ebikecity.project.mode_choice.variables.AstraTripVariables;

public class EBikePtUtilityEstimator extends PtUtilityEstimator {
	static public final String NAME = "AstraPtEstimator";

	private final EBikeModeParameters parameters;
	private final AstraPtPredictor predictor;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;

	@Inject
	public EBikePtUtilityEstimator(EBikeModeParameters parameters, AstraPtPredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateInVehicleTimeUtility(AstraPtVariables variables) {
		double utility = 0.0;

		utility += parameters.astraPt.betaRailTravelTime_u_min //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance) //
				* variables.railTravelTime_min;

		if (variables.railTravelTime_min > 0.0 && variables.busTravelTime_min > 0.0) {
			// This is a feeder case
			utility += parameters.astraPt.betaFeederTravelTime_u_min //
					* variables.busTravelTime_min;
		} else {
			// This is not a feeder case
			utility += parameters.astraPt.betaBusTravelTime_u_min //
					* EstimatorUtils.interaction(variables.euclideanDistance_km,
							parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance) //
					* variables.busTravelTime_min;
		}

		return utility;
	}

	protected double estimateMonetaryCostUtility(AstraPtVariables variables, AstraPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraWalk.betaWork : 0.0;
	}

	protected double estimateHeadwayUtility(AstraPtVariables variables) {
		if (parameters.astraPt.betaHeadway_u_min != 0.0 && variables.headway_min == 0.0) {
			throw new IllegalStateException("Non-zero beta for headway, but no headway is given.");
		}

		return parameters.astraPt.betaHeadway_u_min * variables.headway_min;
	}

	protected double estimateOvgkUtility(AstraPtVariables variables) {
		switch (variables.ovgk) {
		case A:
			return 0.0;
		case B:
			return parameters.astraPt.betaOvgkB_u;
		case C:
			return parameters.astraPt.betaOvgkC_u;
		case D:
			return parameters.astraPt.betaOvgkD_u;
		case None:
			return parameters.astraPt.betaOvgkNone_u;
		default:
			throw new RuntimeException();
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AstraPtVariables variables = predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateInVehicleTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateLineSwitchUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimateHeadwayUtility(variables);
		utility += estimateOvgkUtility(variables);
		
		// List that stores information to be mapped onto trip
		List<String> store = new ArrayList<String>();
		store.add(person.getId().toString());
		store.add(person.getId().toString() + "_" + Integer.toString(trip.getIndex()+1));
		store.add(Double.toString(trip.getDepartureTime()));
		store.add(trip.getOriginActivity().getFacilityId().toString());
		store.add("pt");
		store.add(Double.toString((variables.accessEgressTime_min
									+ variables.inVehicleTime_min
									+ variables.waitingTime_min) * 60));
		store.add(Double.toString(utility));
				
		// How can I make all the estimators add their store into the same container??
		UtilityContainer container = UtilityContainer.getInstance();
		container.getUtilites().add(store);

		return utility;
	}
}
