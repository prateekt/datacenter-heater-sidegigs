package com.heater.analysis;

public record HeatApplicationPoint(
        String scenarioId,
        String label,
        double netCo2eTonnesPerYear,
        double heatPoolMwh,
        double heatAquacultureMwh,
        double heatAlgaeMwh,
        double heatDacMwh,
        double heatTotalMwh,
        double olympicPoolsEquivalent,
        double communityPoolsEquivalent,
        double aquacultureRacewaysEquivalent,
        double fishProductionKgPerYear,
        double algaeHectaresEquivalent,
        double homesHeatedEquivalent,
        double poolSatisfactionPct
) {}
