package com.heater.model;

public final class AlgaeLoad {
    public double optimalTemp = 28.0;
    public double minTemp = 15.0;
    public double maxTemp = 38.0;
    public double volumeM3 = 200.0;
    public double surfaceAreaM2 = 500.0;
    public double maxGrowthGM2Day = 40.0;
    public double co2PerKgBiomass = 1.83;
    public double harvestFraction = 0.80;
    public double lossUa = 600.0;
    public double spargeEfficiency = 0.3;
    public double temperature = 20.0;
    public boolean connected;
    public double co2FixedKg;
    public double currentGrowthRateKgS;
}
