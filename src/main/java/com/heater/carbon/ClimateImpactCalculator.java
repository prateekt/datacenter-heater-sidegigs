package com.heater.carbon;

import com.heater.model.SystemState;

public final class ClimateImpactCalculator {

    private final double gridCo2KgPerKwh;
    private final double climateSensitivity;

    public ClimateImpactCalculator(double gridCo2KgPerKwh, double climateSensitivity) {
        this.gridCo2KgPerKwh = gridCo2KgPerKwh;
        this.climateSensitivity = climateSensitivity;
    }

    public record ClimateReport(
            double dacCo2Kg,
            double algaeCo2Kg,
            double grossRemovalKg,
            double operationalPenaltyKg,
            double netCo2eRemovedKg,
            double annualizedTonnesCo2e,
            double radiativeForcingOffsetWM2,
            double warmingOffsetMilliKelvin,
            double heatPumpElectricKwh,
            double ccsActivePct
    ) {}

    public ClimateReport report(SystemState state, double simDurationS) {
        double dac = state.carbonCapture.co2CapturedKg;
        double algae = state.algae.co2FixedKg;
        double gross = dac + algae;
        double heatPumpKwh = state.heatPumpElectricJ / 3_600_000.0;
        double fanKwh = state.fanElectricJ / 3_600_000.0;
        double penalty = (heatPumpKwh + fanKwh) * gridCo2KgPerKwh;
        double net = gross - penalty;

        double annualFactor = simDurationS > 0 ? (365.0 * 86400.0 / simDurationS) : 0.0;
        double annualizedTonnes = net * annualFactor / 1000.0;

        double forcing = net * 1.75e-15;
        double warmingMk = forcing * climateSensitivity / 3.7 * 1000.0;

        double ccsPct = state.totalSteps > 0 ? 100.0 * state.ccsActiveSteps / state.totalSteps : 0.0;

        return new ClimateReport(
                dac, algae, gross, penalty, net,
                annualizedTonnes, forcing, warmingMk,
                heatPumpKwh, ccsPct
        );
    }
}
