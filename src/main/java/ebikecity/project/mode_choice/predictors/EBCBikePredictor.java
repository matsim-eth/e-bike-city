package ebikecity.project.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.variables.EBCBikeVariables;

public class EBCBikePredictor extends CachedVariablePredictor<EBCBikeVariables> {
	public final BikePredictor delegate;

	@Inject
	public EBCBikePredictor(BikePredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected EBCBikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		BikeVariables delegateVariables = delegate.predictVariables(person, trip, elements);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new EBCBikeVariables(delegateVariables, euclideanDistance_km);
	}
}
