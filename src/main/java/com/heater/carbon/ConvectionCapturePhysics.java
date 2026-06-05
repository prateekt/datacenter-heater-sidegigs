package com.heater.carbon;

public final class ConvectionCapturePhysics {

    private static final double CO2_MOLAR_MASS = 44.01;
    private static final double AIR_MOLAR_MASS = 28.97;
    private static final int MAX_ITER = 20;
    private static final double TOL = 1e-4;

    private ConvectionCapturePhysics() {}

    public record DraftResult(
            double massFlowKgS,
            double volumeFlowM3S,
            double deltaTK,
            double exhaustTempC,
            double buoyancyPressurePa,
            double resistancePressurePa,
            double fanBaselineW,
            double fanResidualW,
            double fanSavedW,
            double captureRateKgS
    ) {}

    public static DraftResult solve(
            ConvectionCaptureConfig cfg,
            double qWasteW,
            double ambientTempC
    ) {
        return solveFromAirHeat(cfg, cfg.wasteHeatToAirFraction * qWasteW, ambientTempC);
    }

    public static DraftResult solveFromAirHeat(
            ConvectionCaptureConfig cfg,
            double qAirW,
            double ambientTempC
    ) {
        double qAir = qAirW;
        if (qAir <= 0 || cfg.bedResistancePaS2Kg2 <= 0 || cfg.chimneyHeightM <= 0) {
            return zeroResult(ambientTempC);
        }

        double tAmbK = ambientTempC + 273.15;
        double mdot = initialGuess(cfg, qAir, tAmbK);

        double deltaT = 0.0;
        for (int i = 0; i < MAX_ITER; i++) {
            deltaT = mdot > 0 ? qAir / (mdot * cfg.cpAir) : 0.0;
            if (deltaT < cfg.minDeltaTK) {
                return zeroResult(ambientTempC);
            }

            double buoyancyPa = buoyancyPressure(cfg, deltaT, tAmbK);
            double mdotNew = Math.sqrt(Math.max(0.0, buoyancyPa / cfg.bedResistancePaS2Kg2));
            if (Math.abs(mdotNew - mdot) < TOL * Math.max(1.0, mdot)) {
                mdot = mdotNew;
                break;
            }
            mdot = 0.5 * mdot + 0.5 * mdotNew;
        }

        if (mdot <= 0 || deltaT < cfg.minDeltaTK) {
            return zeroResult(ambientTempC);
        }

        double buoyancyPa = buoyancyPressure(cfg, deltaT, tAmbK);
        double resistancePa = cfg.bedResistancePaS2Kg2 * mdot * mdot;
        double volFlow = mdot / cfg.airDensityKgM3;
        double exhaustTempC = ambientTempC + deltaT;

        double fanBaselineW = fanPower(mdot, resistancePa, cfg.airDensityKgM3, cfg.fanEfficiency);
        double fanResidualW = fanPower(
                mdot,
                Math.max(0.0, resistancePa - buoyancyPa),
                cfg.airDensityKgM3,
                cfg.fanEfficiency
        );
        double captureRate = captureRateKgS(cfg, mdot);

        return new DraftResult(
                mdot,
                volFlow,
                deltaT,
                exhaustTempC,
                buoyancyPa,
                resistancePa,
                fanBaselineW,
                fanResidualW,
                Math.max(0.0, fanBaselineW - fanResidualW),
                captureRate
        );
    }

    private static double initialGuess(ConvectionCaptureConfig cfg, double qAir, double tAmbK) {
        double deltaTGuess = 15.0;
        double buoyancyPa = buoyancyPressure(cfg, deltaTGuess, tAmbK);
        double mdotFromBuoyancy = Math.sqrt(Math.max(0.0, buoyancyPa / cfg.bedResistancePaS2Kg2));
        double mdotFromEnergy = qAir / (cfg.cpAir * deltaTGuess);
        return Math.max(mdotFromBuoyancy, mdotFromEnergy);
    }

    private static double buoyancyPressure(ConvectionCaptureConfig cfg, double deltaTK, double tAmbK) {
        return cfg.airDensityKgM3 * cfg.gravity * cfg.chimneyHeightM * (deltaTK / tAmbK);
    }

    private static double fanPower(double mdot, double deltaPa, double rho, double efficiency) {
        if (mdot <= 0 || deltaPa <= 0 || efficiency <= 0) {
            return 0.0;
        }
        return mdot * deltaPa / (rho * efficiency);
    }

    private static double captureRateKgS(ConvectionCaptureConfig cfg, double mdot) {
        double co2VolFrac = cfg.ambientCo2Ppm * 1e-6;
        double co2MassFrac = co2VolFrac * (CO2_MOLAR_MASS / AIR_MOLAR_MASS);
        double areaFactor = Math.min(1.0, cfg.contactorAreaM2 / 10_000.0);
        return mdot * co2MassFrac * cfg.captureEfficiency * areaFactor;
    }

    private static DraftResult zeroResult(double ambientTempC) {
        return new DraftResult(0, 0, 0, ambientTempC, 0, 0, 0, 0, 0, 0);
    }
}
