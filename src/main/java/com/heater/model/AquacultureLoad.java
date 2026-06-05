package com.heater.model;

/** Warm-water fishery / aquaculture raceway — pool-like thermal load. */
public final class AquacultureLoad {
    public double volume = 50_000.0;
    public double temperature = 18.0;
    public double setpoint = 22.0;
    public double lossUa = 2500.0;
    public boolean connected;
}
