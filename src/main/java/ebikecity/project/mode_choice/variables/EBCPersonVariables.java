package ebikecity.project.mode_choice.variables;

import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;

public class EBCPersonVariables extends SwissPersonVariables {
	public final double householdIncome_MU;
    public final boolean isFemale;
	public final boolean isUrbaLevel2;
	public final boolean isUrbaLevel3;
	public final int age;

	public EBCPersonVariables(SwissPersonVariables delegate, double householdIncome_MU, boolean isFemale, boolean isUrbaLevel2, boolean isUrbaLevel3, int age) {
		super(delegate, delegate.homeLocation, delegate.hasGeneralSubscription, delegate.hasHalbtaxSubscription,
				delegate.hasRegionalSubscription, delegate.statedPreferenceRegion);
		this.householdIncome_MU = householdIncome_MU;
        this.isFemale = isFemale;
		this.isUrbaLevel2 = isUrbaLevel2;
		this.isUrbaLevel3 = isUrbaLevel3;
		this.age = age;
	}
}
