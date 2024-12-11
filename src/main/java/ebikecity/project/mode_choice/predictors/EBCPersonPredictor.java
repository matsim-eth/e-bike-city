package ebikecity.project.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

import ebikecity.project.mode_choice.variables.EBCPersonVariables;

public class EBCPersonPredictor extends CachedVariablePredictor<EBCPersonVariables> {
	public final SwissPersonPredictor delegate;

	@Inject
	public EBCPersonPredictor(SwissPersonPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected EBCPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double householdIncome_MU = EBCPredictorUtils.getHouseholdIncome(person);
		boolean isFemale          = EBCPredictorUtils.isFemale(person);
		boolean isUrbaLevel2      = EBCPredictorUtils.isUrbaLevel2(person);
		boolean isUrbaLevel3      = EBCPredictorUtils.isUrbaLevel3(person);
		int age                   = EBCPredictorUtils.getAge(person);
		return new EBCPersonVariables(delegate.predictVariables(person, trip, elements), householdIncome_MU, isFemale, isUrbaLevel2, isUrbaLevel3, age);
	}
}
