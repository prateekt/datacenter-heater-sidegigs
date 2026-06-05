package com.heater.analysis;

public record ConvectionSweepPoint(
        String sweepId,
        String label,
        double chimneyHeightM,
        double contactorAreaM2,
        double wasteHeatToAirFraction,
        double avgWasteHeatMw,
        double airflowM3S,
        double fanBaselineMw,
        double fanResidualMw,
        double fanSavedMw,
        double grossCo2TonnesYr,
        double netCo2TonnesYr,
        double deltaTK,
        double captureRateKgS
) {}
