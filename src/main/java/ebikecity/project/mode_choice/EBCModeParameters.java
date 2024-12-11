package ebikecity.project.mode_choice;

import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class EBCModeParameters extends SwissModeParameters {
	static public class EBCBaseModeParameters {
		public double age = 0.0;
		public double female = 0.0;
		public double urbLevel2 = 0.0;
		public double urbLevel3 = 0.0;
		public double beta_externalities = 0.0;
		public double ext_by_km = 0.0;
		public double beta_cost = 0.0;

		public double alpha = 0.0;
		public double betaTravelTime_min = 0.0;
		public double beta_parking_cost = 0.0;
	}

	public EBCBaseModeParameters ebcWalk = new EBCBaseModeParameters();
	public EBCBaseModeParameters ebcBike = new EBCBaseModeParameters();
	public EBCBaseModeParameters ebcEBike = new EBCBaseModeParameters();
	public EBCBaseModeParameters ebcSpedelec = new EBCBaseModeParameters();
	public EBCBaseModeParameters ebcCar = new EBCBaseModeParameters();

	public class EBCPtParameters {
		public double betaTravelTime_min = 0.0;
		public double betaHeadway_min = 0.0;
		public double age = 0.0;
		public double female = 0.0;
		public double urbLevel2 = 0.0;
		public double urbLevel3 = 0.0;
		public double beta_externalities = 0.0;
		public double ext_by_km = 0.0;
		public double beta_cost = 0.0;
	}

	public EBCPtParameters ebcPt = new EBCPtParameters();
	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public EBCModeParameters modeParamInclEBike() {
		EBCModeParameters parameters = new EBCModeParameters();

		// General
		parameters.betaCost_u_MU = -0.0888;
		parameters.lambdaCostHouseholdIncome = -0.8169;
		parameters.lambdaCostEuclideanDistance = -0.2209;
		parameters.lambdaTravelTimeEuclideanDistance = 0.1147;
		parameters.referenceEuclideanDistance_km = 39.0;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Public transport
		parameters.pt.alpha_u = -0.8;//-0.5;//-1.34541;
		parameters.pt.betaAccessEgressTime_u_min = -1.96973;
		parameters.ebcPt.betaTravelTime_min = -2.0;//-1.0;//-2.0;//-2.5;//-3.21258;
		parameters.ebcPt.betaHeadway_min = -0.50346;
		parameters.ebcPt.age = 0.00354;
		parameters.ebcPt.female = 0.31345;
		parameters.ebcPt.urbLevel2 = -0.94476;
		parameters.ebcPt.urbLevel3 = -1.25242;
		parameters.ebcPt.beta_externalities = 1.44709;
		parameters.ebcPt.ext_by_km = 0.08;
		parameters.ebcPt.beta_cost = -0.06934;		

		// Bicycle
		parameters.bike.alpha_u = 1.4;//1.6;//1.22846;
		parameters.bike.betaTravelTime_u_min = -2.4;//-2.0;//-3.68053;
		parameters.ebcBike.age = -0.02074;
		parameters.ebcBike.female = -0.03147;
		parameters.ebcBike.urbLevel2 = -1.29194;
		parameters.ebcBike.urbLevel3 = -1.92303;
		parameters.ebcBike.beta_externalities = 3.18593;
		parameters.ebcBike.ext_by_km = -0.0364;

		// Ebike
		parameters.ebcEBike.alpha = -0.8;//-1.2;//-1.64421;
		parameters.ebcEBike.betaTravelTime_min = -2.0;//-2.78651;
		parameters.ebcEBike.age = 0.00268;
		parameters.ebcEBike.female = 0.3291;
		parameters.ebcEBike.urbLevel2 = -0.51416;
		parameters.ebcEBike.urbLevel3 = -0.64266;
		parameters.ebcEBike.beta_externalities = 0;
		parameters.ebcEBike.ext_by_km = 0.0264;

		// Spedelec
		parameters.ebcSpedelec.alpha = -1.4;//-2.0;//-2.550656;
		parameters.ebcSpedelec.betaTravelTime_min = -0.3;//-0.774801;
		parameters.ebcSpedelec.age = -0.026566;
		parameters.ebcSpedelec.female = -0.363751;
		parameters.ebcSpedelec.urbLevel2 = -0.464518;
		parameters.ebcSpedelec.urbLevel3 = -0.651147;
		parameters.ebcSpedelec.beta_externalities = 0;
		parameters.ebcSpedelec.ext_by_km = 0.0264;

		// Car
		parameters.car.alpha_u = 0.3;//0;//-0.3;//-0.5;//0;
		parameters.car.betaTravelTime_u_min = -6.0;//-5.5;//-4.7;//-4.376983;
		parameters.ebcCar.beta_externalities = 0.644314;
		parameters.ebcCar.ext_by_km = 0.1601;
		parameters.ebcCar.beta_cost = -0.06934;
		parameters.ebcCar.beta_parking_cost = -0.305164;

		// Walking
		parameters.walk.alpha_u = 0.8;//1.0;//1.91847;
		parameters.walk.betaTravelTime_u_min = -2.0;//-4.0;//-5.52097;
		parameters.ebcWalk.age = -0.00447;
		parameters.ebcWalk.female = 0.07087;
		parameters.ebcWalk.urbLevel2 = -0.70167;
		parameters.ebcWalk.urbLevel3 = -0.37095;
		parameters.ebcWalk.beta_externalities = 0.0;//3.0;//13.6768;
		parameters.ebcWalk.ext_by_km = -0.0997;

		return parameters;
	}
}
