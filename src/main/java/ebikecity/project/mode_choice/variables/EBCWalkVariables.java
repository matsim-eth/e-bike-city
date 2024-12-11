package ebikecity.project.mode_choice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;

public class EBCWalkVariables extends WalkVariables {
	final public double euclideanDistance_km;

	public EBCWalkVariables(WalkVariables delegate, double euclideanDistance_km) {
		super(delegate.travelTime_min);
		this.euclideanDistance_km = euclideanDistance_km;
	}
}
