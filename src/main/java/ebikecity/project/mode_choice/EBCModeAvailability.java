package ebikecity.project.mode_choice;

import java.util.Collection;
import java.util.List;

import org.eqasim.switzerland.mode_choice.SwissModeAvailability;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;


public class EBCModeAvailability implements ModeAvailability {
	public static final String NAME = "EBCModeAvailability";

	private final SwissModeAvailability delegate;

	public EBCModeAvailability(SwissModeAvailability delegate) {
		this.delegate = delegate;
	}

	@Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = delegate.getAvailableModes(person, trips);

        if (person.getAttributes().getAttribute("isFreight").toString().equals("false")) {
            if (person.getAttributes().getAttribute("bikeAvailability").equals("AVAILABLE")) {
                modes.add(TransportMode.bike);
            }
            if (person.getAttributes().getAttribute("ebikeAvailability").equals("AVAILABLE")) {
                modes.add("ebike");
            }
            if (person.getAttributes().getAttribute("spedelecAvailability").equals("AVAILABLE")) {
                modes.add("spedelec");
            }
        }

        return modes;
	}
}
