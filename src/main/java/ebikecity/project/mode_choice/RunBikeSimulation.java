package ebikecity.project.mode_choice;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ebikecity.project.config.AstraConfigurator;
import ebikecity.project.travel_time.SmoothingTravelTimeModule;

public class RunBikeSimulation {
	
	
	private static final String BIKE=TransportMode.bike;
	
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes( "mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), AstraConfigurator.getConfigGroups());
		AstraConfigurator.configure(config);
		cmd.applyConfiguration(config);
		
		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);
		AstraConfigurator.adjustScenario(scenario);

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
		
		// add allowed mode bike for car links that are not highway or trunk
		
		NetworkFilterManager n = new NetworkFilterManager(scenario.getNetwork());
		n.addLinkFilter(new NetworkLinkFilter() {
					
			@Override
			public boolean judgeLink(Link l) {
				return l.getAllowedModes().contains("car");
			}
		});
		
		Network net = n.applyFilters();
		
		for (Link carLink : net.getLinks().values())  {
			String roadType = carLink.getAttributes().getAttribute("osm:way:highway").toString();
			
			if (!(roadType.contains("motorway") || roadType.contains("trunk"))) {
				carLink.setAllowedModes(new HashSet<>(Arrays.asList(TransportMode.car,"bike")));
			}
		}
		
		// create all vehicleTypes requested by planscalcroute networkModes

		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class))
				.setMaximumVelocity(120.0/3.6));
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create("car_passenger", VehicleType.class))
				.setMaximumVelocity(120.0/3.6));
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create(TransportMode.truck, VehicleType.class))
				// look this up later to be consistent
				.setMaximumVelocity(80.0/3.6).setPcuEquivalents(2.5));
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(BIKE, VehicleType.class))
				.setMaximumVelocity(15.0/3.6).setPcuEquivalents(0.25)); 
		
		

		// EqasimLinkSpeedCalculator deactivated!

		Controler controler = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controler);
		controler.addOverridingModule(new EqasimAnalysisModule());
		controler.addOverridingModule(new EqasimModeChoiceModule());
		controler.addOverridingModule(new SwissModeChoiceModule(cmd));
		controler.addOverridingModule(new AstraModule(cmd));

		AstraConfigurator.configureController(controler, cmd);

		controler.addOverridingModule(new SmoothingTravelTimeModule());
		
		// copy from RunBicycleExpresswayExample
		
		controler.addOverridingModule( new AbstractModule(){
			// preparation: compute max speed given link speed limit and vehicle maximum speed:
			private double getMaxSpeedFromVehicleAndLink( Link link, double time, Vehicle vehicle ) {

				double maxSpeedFromLink = link.getFreespeed( time );

				double maxSpeedFromVehicle = vehicle.getType().getMaximumVelocity();
				
				// as usual, we return the min of all the speeds:
				return Math.min ( maxSpeedFromLink, maxSpeedFromVehicle );
			}

			@Override public void install(){

				// set meaningful travel time binding for routing:
				this.addTravelTimeBinding( BIKE ).toInstance( new TravelTime(){
					@Inject @Named(BIKE) TravelTimeCalculator bikeCalculator ;
					// (not very obvious why this is the correct syntax.  kai, jan'23)

					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){

						// we get the max speed from vehicle and link, as defined in the preparation above:
						final double maxSpeedFromVehicleAndLink = getMaxSpeedFromVehicleAndLink( link, time, vehicle );

						// we also get the speed from observation:
						double speedFromObservation = bikeCalculator.getLinkTravelTimes().getLinkTravelTime( link, time, person, vehicle );

						// we compute the min of the two:
						double actualSpeed = Math.min( speedFromObservation, maxSpeedFromVehicleAndLink );

						// the link travel time is computed from that speed:
						return link.getLength()/actualSpeed ;
					}
				} );

				// make the qsim such that bicycle son bicycle expressways are faster than their normal speed:
				
				// changed for MATSim 13
				// this.installOverridingQSimModule( new AbstractQSimModule(){
				this.installQSimModule( new AbstractQSimModule(){
					@Inject EventsManager events;
					@Inject Scenario scenario;
					@Override protected void configureQSim(){
						// instantiate the configurable network factory:
						final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);

						// set the speed calculation as declared above in the preparation:
						factory.setLinkSpeedCalculator( ( qVehicle, link, time ) -> getMaxSpeedFromVehicleAndLink( link, time, qVehicle.getVehicle() ) );

						// set (= overwrite) the QNetworkFactory with the factory defined here:
						bind( QNetworkFactory.class ).toInstance(factory );
						// (this is a bit dangerous since other pieces of code might overwrite the QNetworkFactory as well.  In the longer run, need to find a different solution.)
					}
				} );
			}
		} ) ;


		controler.run();
	}
}