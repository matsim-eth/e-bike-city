package ebikecity.utils.impute_headway;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRouteFactory;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.inject.Injector;

import ebikecity.utils.impute_headway.headway.HeadwayImputer;
import ebikecity.utils.impute_headway.headway.HeadwayImputerModule;

public class RunImputeHeadway {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "output-path") //
                .allowOptions("threads", "batch-size") //
                .build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
                EqasimConfigurator.getConfigGroups());
        cmd.applyConfiguration(config);
        config.strategy().clearStrategySettings();

        int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
        int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
                .orElse(Runtime.getRuntime().availableProcessors());

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(TransitPassengerRoute.class, new DefaultTransitPassengerRouteFactory());
        ScenarioUtils.loadScenario(scenario);

        Injector injector = new InjectorBuilder(scenario) //
                .addOverridingModules(EqasimConfigurator.getModules()) //
                .addOverridingModule(new HeadwayImputerModule(numberOfThreads, batchSize, true, 1.0 * 3600.0)) //
                .build();

        HeadwayImputer headwayImputer = injector.getInstance(HeadwayImputer.class);
        headwayImputer.run(scenario.getPopulation());

        new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
	}
}
