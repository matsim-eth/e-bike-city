package ebikecity.project.config;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.households.Household;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ebikecity.project.mode_choice.EBCModeAvailability;
import ebikecity.project.mode_choice.InfiniteHeadwayConstraint;
import ebikecity.project.mode_choice.estimators.EBCBikeUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCCarUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCEBikeUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCPtUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCWalkUtilityEstimator;
import ebikecity.project.mode_choice.estimators.EBCSpedelecUtilityEstimator;


public class EBCConfigurator extends EqasimConfigurator {
	public EBCConfigurator() {
	}

	public ConfigGroup[] getConfigGroups() {
		return new ConfigGroup[] { //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new EBCConfigGroup()
		};
	}

	static public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		
		config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

		// General eqasim
		eqasimConfig.setAnalysisInterval(config.controler().getWriteEventsInterval());

		// Estimators
		eqasimConfig.setEstimator(TransportMode.car, EBCCarUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.pt, EBCPtUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.bike, EBCBikeUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.walk, EBCWalkUtilityEstimator.NAME);
		eqasimConfig.setEstimator("ebike", EBCEBikeUtilityEstimator.NAME);
		eqasimConfig.setEstimator("spedelec", EBCSpedelecUtilityEstimator.NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.add(InfiniteHeadwayConstraint.NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		dmcConfig.setModeAvailability(EBCModeAvailability.NAME);
	}	

	public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
				}
			}
		}		
		adjustBikeAvailability(scenario);
	}


static private void adjustBikeAvailability(Scenario scenario) {
  Random random = new Random(scenario.getConfig().global().getRandomSeed());
  double chance = 0.7; // Set the chance to 0.7

  for (Person person : scenario.getPopulation().getPersons().values()) {
    if (!person.getId().toString().contains("freight")) {
      if (random.nextDouble() <= chance) {
        person.getAttributes().putAttribute("bikeAvailability", "AVAILABLE");
        person.getAttributes().putAttribute("ebikeAvailability", "AVAILABLE");
        person.getAttributes().putAttribute("spedelecAvailability", "AVAILABLE");
      } else {
        person.getAttributes().putAttribute("bikeAvailability", "FOR_NONE");
        person.getAttributes().putAttribute("ebikeAvailability", "FOR_NONE");
        person.getAttributes().putAttribute("spedelecAvailability", "FOR_NONE");
      }
    }
  }
}

	static public void configureController(Controler controller, CommandLine commandLine) {		

		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator, controller.getConfig());
		});
	}
}