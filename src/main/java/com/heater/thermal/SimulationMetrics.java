package com.heater.thermal;

import com.heater.carbon.ClimateImpactCalculator;
import com.heater.robot.RobotTaskLog;

import java.util.List;

public record SimulationMetrics(
        double energyRecoveredKwh,
        double energyRejectedKwh,
        double primaryUnsafeTimeS,
        double poolSatisfactionPct,
        double houseSatisfactionPct,
        int steps,
        List<RobotTaskLog> robotEvents,
        ClimateImpactCalculator.ClimateReport climate,
        double convectionAirflowM3s,
        double fanPowerSavedMw,
        double convectionCo2CapturedTonnesYr
) {}
