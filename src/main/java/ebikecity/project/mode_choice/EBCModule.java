package ebikecity.project.mode_choice;

import java.io.File;
import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ebikecity.project.mode_choice.estimators.EBCBikeUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCCarUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCEBikeUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCSpedelecUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCPtUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCWalkUtilityEstimator;
import ebikecity.project.mode_choice.predictors.EBCAccessEgressBikePredictor;
import ebikecity.project.mode_choice.predictors.AccessEgressCarPredictor;
import ebikecity.project.mode_choice.predictors.EBCBikePredictor;
import ebikecity.project.mode_choice.predictors.EBCPersonPredictor;
import ebikecity.project.mode_choice.predictors.EBCPtPredictor;
import ebikecity.project.mode_choice.predictors.EBCTripPredictor;
import ebikecity.project.mode_choice.predictors.EBCWalkPredictor;

public class EBCModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public EBCModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {

		bindUtilityEstimator(EBCCarUtilityEstimator.NAME).to(EBCCarUtilityEstimator.class);
		bindUtilityEstimator(EBCPtUtilityEstimator.NAME).to(EBCPtUtilityEstimator.class);
		bindUtilityEstimator(EBCBikeUtilityEstimator.NAME).to(EBCBikeUtilityEstimator.class);
		bindUtilityEstimator(EBCWalkUtilityEstimator.NAME).to(EBCWalkUtilityEstimator.class);		
		bindUtilityEstimator(EBCEBikeUtilityEstimator.NAME).to(EBCEBikeUtilityEstimator.class);
		bindUtilityEstimator(EBCSpedelecUtilityEstimator.NAME).to(EBCSpedelecUtilityEstimator.class);

		bind(EBCPtPredictor.class);
		bind(EBCAccessEgressBikePredictor.class);
		bind(EBCTripPredictor.class);
		bind(EBCWalkPredictor.class);
		bind(EBCPersonPredictor.class);
		bind(AccessEgressCarPredictor.class);

		bindTripConstraintFactory(InfiniteHeadwayConstraint.NAME).to(InfiniteHeadwayConstraint.Factory.class);

		bind(SwissModeParameters.class).to(EBCModeParameters.class);

		bind(SwissModeAvailability.class);
		
		bindModeAvailability(EBCModeAvailability.NAME).to(EBCModeAvailability.class);

	}

	@Provides
	@Singleton
	public EBCModeParameters provideEBCModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		EBCModeParameters parameters = EBCModeParameters.modeParamInclEBike();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public OVGKCalculator provideOVGKCalculator(TransitSchedule transitSchedule) {
		return new OVGKCalculator(transitSchedule);
	}

	@Provides
	public EBCModeAvailability provideEBCModeAvailability(SwissModeAvailability delegate) {
		return new EBCModeAvailability(delegate);
	}
}