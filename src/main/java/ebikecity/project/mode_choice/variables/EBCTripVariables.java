package ebikecity.project.mode_choice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class EBCTripVariables implements BaseVariables {
	public final boolean isWork;
	public final boolean isHome;
	public final boolean isCity;
    public final double networkDistance_km;
	public final double parkingDuration;

	public EBCTripVariables(boolean isWork, boolean isHome, boolean isCity, double distance_km, double parkingDuration) {
		this.isWork = isWork;
		this.isHome = isHome;
		this.isCity = isCity;
        this.networkDistance_km = distance_km;
		this.parkingDuration = parkingDuration;
	}
}
