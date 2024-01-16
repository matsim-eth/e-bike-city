package ebikecity.project.travel_time;

import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

public class TimeBinControlerListener implements IterationStartsListener {

	private final TravelTimeCalculatorConfigGroup ttcConfig;
	
	@Inject
	public TimeBinControlerListener(TravelTimeCalculatorConfigGroup ttcConfig) {
		this.ttcConfig = ttcConfig;
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == 0) {
			ttcConfig.setTraveltimeBinSize(60*60);
			
		}
		if (event.getIteration() == 20) {
			ttcConfig.setTraveltimeBinSize(15*60);
			
		}
		
	}
	

}
