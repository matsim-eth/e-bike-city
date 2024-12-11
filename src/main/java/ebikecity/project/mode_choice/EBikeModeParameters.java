package ebikecity.project.mode_choice;


public class EBikeModeParameters extends AstraModeParameters{
	static public class EBikeBaseModeParameters {
		public double betaAgeOver60 = 0.0;
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
		

		public double betaWork = 0.0;
		public double betaCity = 0.0;
		public double travelTimeThreshold_min = 0.0;
	}
	
	public EBikeBaseModeParameters ebike = new EBikeBaseModeParameters();
	
	static public EBikeModeParameters modeParamInclEBike() {
		EBikeModeParameters parameters = new EBikeModeParameters();

		// General
		parameters.betaCost_u_MU = -0.0888;

		parameters.lambdaCostHouseholdIncome = -0.8169;
		parameters.lambdaCostEuclideanDistance = -0.2209;
		parameters.lambdaTravelTimeEuclideanDistance = 0.1147;

		parameters.referenceEuclideanDistance_km = 39.0;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Public transport
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaWaitingTime_u_min = -0.0124;
		parameters.pt.betaAccessEgressTime_u_min = -0.0142;

		parameters.astraPt.betaFeederTravelTime_u_min = -0.0452;
		parameters.astraPt.betaBusTravelTime_u_min = -0.0124;
		parameters.astraPt.betaRailTravelTime_u_min = -0.0072;
		parameters.astraPt.betaHeadway_u_min = -0.0301;

		parameters.astraPt.betaOvgkB_u = -1.7436;
		parameters.astraPt.betaOvgkC_u = -1.6413;
		parameters.astraPt.betaOvgkD_u = -0.9649;
		parameters.astraPt.betaOvgkNone_u = -1.0889;

		// Bicycle
		parameters.bike.alpha_u = 0.1522;
		parameters.bike.betaTravelTime_u_min = -0.1258;

		parameters.astraBike.betaAgeOver60 = -2.6588;
		
		// E-Bike
		parameters.ebike.alpha_u = 0.1522;
		parameters.ebike.betaTravelTime_u_min = -0.1258;

		parameters.ebike.betaAgeOver60 = -2.6588;

		// Car
		parameters.car.alpha_u = -0.8; // Original from fb model: 0.2235;
		parameters.car.betaTravelTime_u_min = -0.0192;

		parameters.astraCar.betaWork = -1.1606;
		parameters.astraCar.betaCity = -0.4590;

		// Walking
		parameters.walk.alpha_u = 0.5903;
		parameters.walk.betaTravelTime_u_min = -0.0457;

		parameters.astraWalk.travelTimeThreshold_min = 120.0;

		return parameters;
	}

}
