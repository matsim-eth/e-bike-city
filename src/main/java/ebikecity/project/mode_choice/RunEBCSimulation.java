package ebikecity.project.mode_choice;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ebikecity.project.mode_choice.estimators.UtilityControlerListener;
import ebikecity.project.config.EBCConfigurator;
import ebikecity.project.travel_time.EBCSmoothingTravelTimeModule;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;

public class RunEBCSimulation {	
	
	private static final String BIKE=TransportMode.bike;
	
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes( "mode-parameter", "cost-parameter", "preventwaitingtoentertraffic") //
				.build();

		EBCConfigurator ebcConfigurator = new EBCConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), ebcConfigurator.getConfigGroups());
		ebcConfigurator.configure(config);
		cmd.applyConfiguration(config);
		
		if (cmd.hasOption("preventwaitingtoentertraffic")) {
			if (cmd.getOption("preventwaitingtoentertraffic").get().equals("y")) {
				System.out.println("Preventing Waiting To Enter Traffic switched to YES");
				((QSimConfigGroup) config.getModules().get(QSimConfigGroup.GROUP_NAME))
						.setPcuThresholdForFlowCapacityEasing(1.0);
			}
		}
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		for (Person person : scenario.getPopulation().getPersons().values()) {			
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					if (((Leg) pe).getMode() == BIKE) {
						((Leg) pe).setRoute(null);
					}
				}
			}
		}		

		SwitzerlandConfigurator switzerlandConfigurator = new SwitzerlandConfigurator();
		switzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		switzerlandConfigurator.adjustScenario(scenario);
		ebcConfigurator.adjustScenario(scenario);

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		
		config.plansCalcRoute().setAccessEgressType( AccessEgressType.accessEgressModeToLink );

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
		
		// set config such that the mode vehicles come from vehicles data:
		scenario.getConfig().qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );	
			
		// create all vehicleTypes requested by planscalcroute networkModes

		final VehiclesFactory vf = VehicleUtils.getFactory();
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class))
				.setMaximumVelocity(120.0/3.6));
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create("car_passenger", VehicleType.class))
				.setMaximumVelocity(120.0/3.6));
		scenario.getVehicles().addVehicleType(vf.createVehicleType(Id.create(TransportMode.truck, VehicleType.class))
				.setMaximumVelocity(80.0/3.6).setPcuEquivalents(2.5));
		// cannot set networkMode to bike, but works anyway (bikes and ebikes use paths, car not)
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create(BIKE, VehicleType.class))
				.setMaximumVelocity(15.0/3.6).setPcuEquivalents(0.25)); 
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("ebike", VehicleType.class))
				.setMaximumVelocity(25.0/3.6).setPcuEquivalents(0.25));	
		scenario.getVehicles().addVehicleType( vf.createVehicleType(Id.create("spedelec", VehicleType.class))
				.setMaximumVelocity(45.0/3.6).setPcuEquivalents(0.25));	
				
		Set<String> networkModes = new HashSet<>(config.plansCalcRoute().getNetworkModes());
		System.out.println("Here are the network modes");
		for (String mode : networkModes) {
    			System.out.println("- " + mode);
    		}
	

		// EqasimLinkSpeedCalculator deactivated!

		Controler controler = new Controler(scenario);
		switzerlandConfigurator.configureController(controler);
		controler.addOverridingModule(new EqasimAnalysisModule());
		controler.addOverridingModule(new EqasimModeChoiceModule());
		controler.addOverridingModule(new SwissModeChoiceModule(cmd));
		controler.addOverridingModule(new EBCModule(cmd));
		
		ebcConfigurator.configureController(controler, cmd);

		controler.addOverridingModule(new EBCSmoothingTravelTimeModule());
		
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
						// return link.getLength()/actualSpeed ;
						String vod = link.getAttributes().getAttribute("osm:way:cost_cycling_>").toString();
						double vodd = 1000000000;
						if (!vod.equals("inf")) {
							vodd = Double.parseDouble(vod);
						}
						
						return vodd/actualSpeed;
					}
				} );
				
				this.addTravelTimeBinding( "ebike" ).toInstance( new TravelTime(){
					@Inject @Named("ebike") TravelTimeCalculator eBikeCalculator ;
					// (not very obvious why this is the correct syntax.  kai, jan'23)

					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){

						// we get the max speed from vehicle and link, as defined in the preparation above:
						final double maxSpeedFromVehicleAndLink = getMaxSpeedFromVehicleAndLink( link, time, vehicle );

						// we also get the speed from observation:
						double speedFromObservation = eBikeCalculator.getLinkTravelTimes().getLinkTravelTime( link, time, person, vehicle );

						// we compute the min of the two:
						double actualSpeed = Math.min( speedFromObservation, maxSpeedFromVehicleAndLink );

						// the link travel time is computed from that speed:
						// return link.getLength()/actualSpeed ;
						String vod = link.getAttributes().getAttribute("osm:way:cost_cycling_>").toString();
						double vodd = 1000000000;
						if (!vod.equals("inf")) {
							vodd = Double.parseDouble(vod);
						}
						
						return vodd/actualSpeed;
					}
				} );

				this.addTravelTimeBinding( "spedelec" ).toInstance( new TravelTime(){
					@Inject @Named("spedelec") TravelTimeCalculator spedelecCalculator ;
					// (not very obvious why this is the correct syntax.  kai, jan'23)

					@Override public double getLinkTravelTime( Link link, double time, Person person, Vehicle vehicle ){

						// we get the max speed from vehicle and link, as defined in the preparation above:
						final double maxSpeedFromVehicleAndLink = getMaxSpeedFromVehicleAndLink( link, time, vehicle );

						// we also get the speed from observation:
						double speedFromObservation = spedelecCalculator.getLinkTravelTimes().getLinkTravelTime( link, time, person, vehicle );

						// we compute the min of the two:
						double actualSpeed = Math.min( speedFromObservation, maxSpeedFromVehicleAndLink );

						// the link travel time is computed from that speed:
						// return link.getLength()/actualSpeed ;
						String vod = link.getAttributes().getAttribute("osm:way:cost_cycling_>").toString();
						double vodd = 1000000000;
						if (!vod.equals("inf")) {
							vodd = Double.parseDouble(vod);
						}
						
						return vodd/actualSpeed;
					}
				} );

			}
		} ) ;
		controler.run();
	}
}
