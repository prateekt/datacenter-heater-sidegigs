package com.heater.carbon;

import com.heater.model.ActuatorState;
import com.heater.model.AlgaeLoad;
import com.heater.model.SystemState;

public final class AlgaeGrowthModel {

    private static final double PI = Math.PI;

    private AlgaeGrowthModel() {}

    public static double tempFactor(AlgaeLoad algae, double pondTemp) {
        if (pondTemp < algae.minTemp || pondTemp > algae.maxTemp) {
            return 0.0;
        }
        double diff = pondTemp - algae.optimalTemp;
        double width = 6.0;
        return Math.exp(-(diff * diff) / (2.0 * width * width));
    }

    public static double lightFactor(double simTimeSeconds) {
        double hour = (simTimeSeconds / 3600.0) % 24.0;
        double sin = Math.sin(PI * hour / 24.0);
        double v = Math.max(0.0, sin);
        return v * v;
    }

    public static double co2EnrichmentFactor(AlgaeLoad algae, double dacCaptureRateKgS) {
        double boost = Math.min(0.5, dacCaptureRateKgS * algae.spargeEfficiency);
        return 1.0 + boost;
    }

    public static double integrateGrowth(
            AlgaeLoad algae,
            double simTime,
            double dacCaptureRateKgS,
            double dt
    ) {
        if (!algae.connected) {
            algae.currentGrowthRateKgS = 0.0;
            return 0.0;
        }
        double tf = tempFactor(algae, algae.temperature);
        double lf = lightFactor(simTime);
        double cf = co2EnrichmentFactor(algae, dacCaptureRateKgS);

        double growthKgS = (algae.maxGrowthGM2Day / 86400.0 / 1000.0)
                * algae.surfaceAreaM2
                * tf
                * lf
                * cf
                * algae.harvestFraction;

        algae.currentGrowthRateKgS = growthKgS;
        double co2Rate = growthKgS * algae.co2PerKgBiomass;
        algae.co2FixedKg += co2Rate * dt;
        return co2Rate;
    }

    public static void updateThermal(
            AlgaeLoad algae,
            double ambientTemp,
            ActuatorState actuators,
            double cpWater,
            double dt
    ) {
        double qLoss = algae.lossUa * (algae.temperature - ambientTemp);
        double qGain = 0.0;
        if (algae.connected && actuators.algaeValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.35 * cpWater
                    * Math.max(0.0, algae.optimalTemp - algae.temperature);
        }
        double mass = algae.volumeM3 * 1000.0;
        algae.temperature += (qGain - qLoss) * dt / (mass * cpWater);
    }
}
