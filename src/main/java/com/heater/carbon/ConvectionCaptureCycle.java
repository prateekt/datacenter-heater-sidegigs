package com.heater.carbon;

public final class ConvectionCaptureCycle {

    public enum Phase {
        ADSORB,
        REGENERATE
    }

    private ConvectionCaptureCycle() {}

    public record CycleResult(
            Phase phase,
            double phaseFraction,
            double adsorbCaptureKgPerCycle,
            double regenCo2KgPerCycle,
            double regenHeatPumpKwhPerCycle,
            double fanResidualKwhPerCycle,
            double fanSavedKwhPerCycle,
            double netCo2KgPerCycle,
            double annualizedGrossTonnes,
            double annualizedNetTonnes,
            double annualizedFanSavedMwh,
            double avgAirflowM3S
    ) {}

    public static CycleResult evaluate(
            ConvectionCaptureConfig cfg,
            ConvectionCapturePhysics.DraftResult draft,
            double sourceTempC,
            double simDurationS
    ) {
        double cycleHours = cfg.adsorbHours + cfg.regenHours;
        if (cycleHours <= 0) {
            return emptyResult();
        }

        double adsorbFrac = cfg.adsorbHours / cycleHours;
        double regenFrac = cfg.regenHours / cycleHours;
        double cycleSeconds = cycleHours * 3600.0;

        double adsorbCaptureKg = draft.captureRateKgS() * cfg.adsorbHours * 3600.0;
        double regenCo2Kg = sourceTempC >= cfg.minSourceTemp ? adsorbCaptureKg : 0.0;
        double grossKgPerCycle = adsorbCaptureKg;

        double fanResidualKwh = wattHoursToKwh(draft.fanResidualW(), cfg.adsorbHours);
        double fanSavedKwh = wattHoursToKwh(draft.fanSavedW(), cfg.adsorbHours);
        double regenHeatPumpKwh = regenHeatPumpKwhForMass(cfg, regenCo2Kg);

        double fanPenaltyKg = (fanResidualKwh + regenHeatPumpKwh) * cfg.gridCo2KgPerKwh;
        double netKgPerCycle = grossKgPerCycle - fanPenaltyKg;

        double cyclesPerYear = simDurationS > 0
                ? (365.0 * 86400.0 / simDurationS) * (simDurationS / cycleSeconds)
                : (365.0 * 24.0 / cycleHours);

        return new CycleResult(
                Phase.ADSORB,
                adsorbFrac,
                adsorbCaptureKg,
                regenCo2Kg,
                regenHeatPumpKwh,
                fanResidualKwh,
                fanSavedKwh,
                netKgPerCycle,
                grossKgPerCycle * cyclesPerYear / 1000.0,
                netKgPerCycle * cyclesPerYear / 1000.0,
                fanSavedKwh * cyclesPerYear / 1000.0,
                draft.volumeFlowM3S() * adsorbFrac
        );
    }

    public static Phase phaseAtTime(double timeS, ConvectionCaptureConfig cfg) {
        double cycleS = (cfg.adsorbHours + cfg.regenHours) * 3600.0;
        if (cycleS <= 0) {
            return Phase.ADSORB;
        }
        double t = timeS % cycleS;
        return t < cfg.adsorbHours * 3600.0 ? Phase.ADSORB : Phase.REGENERATE;
    }

    private static double regenHeatPumpKwhForMass(ConvectionCaptureConfig cfg, double co2Kg) {
        if (co2Kg <= 0 || cfg.heatPumpCop <= 0 || cfg.specificHeatDutyJPerKg <= 0) {
            return 0.0;
        }
        double regenHeatJ = co2Kg * cfg.specificHeatDutyJPerKg;
        return regenHeatJ / cfg.heatPumpCop / 3_600_000.0;
    }

    private static double wattHoursToKwh(double watts, double hours) {
        return watts * hours / 1000.0;
    }

    private static CycleResult emptyResult() {
        return new CycleResult(
                Phase.ADSORB, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }
}
