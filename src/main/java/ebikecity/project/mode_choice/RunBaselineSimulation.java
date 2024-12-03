package ebikecity.project.mode_choice;

import java.io.IOException;
import java.net.MalformedURLException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ebikecity.project.config.AstraConfigurator;
import ebikecity.project.travel_time.SmoothingTravelTimeModule;

public class RunBaselineSimulation {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes( "mode-parameter", "cost-parameter", "preventwaitingtoentertraffic") //
				.build();

		AstraConfigurator astraConfigurator = new AstraConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), astraConfigurator.getConfigGroups());
		astraConfigurator.configure(config);
		cmd.applyConfiguration(config);
		
		if (cmd.hasOption("preventwaitingtoentertraffic")) {
			if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
				System.out.println("Preventing Waiting To Enter Traffic switched to YES");
				((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
						.setPcuThresholdForFlowCapacityEasing(1.0);
			}
		}
		
		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator switzerlandConfigurator = new SwitzerlandConfigurator();
		switzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		switzerlandConfigurator.adjustScenario(scenario);
		astraConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			double maximumSpeed = link.getFreespeed();
			boolean isMajor = true;

			for (Link other : link.getToNode().getInLinks().values()) {
				if (other.getCapacity() >= link.getCapacity()) {
					isMajor = false;
				}
			}

			if (!isMajor && link.getToNode().getInLinks().size() > 1) {
				double travelTime = link.getLength() / maximumSpeed;
				travelTime += eqasimConfig.getCrossingPenalty();
				link.setFreespeed(link.getLength() / travelTime);
			}
		}

		// EqasimLinkSpeedCalcilator deactivated!

		Controler controller = new Controler(scenario);
		switzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));
		controller.addOverridingModule(new AstraModule(cmd));

		AstraConfigurator.configureController(controller, cmd);

		controller.addOverridingModule(new SmoothingTravelTimeModule());

		controller.run();
	}
}
