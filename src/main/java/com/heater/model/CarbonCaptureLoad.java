package com.heater.model;

public final class CarbonCaptureLoad {
    public double regenerationTemp = 90.0;
    public double minSourceTemp = 40.0;
    public double heatPumpCop = 3.0;
    public double specificHeatDutyJPerKg = 5_500_000.0;
    public double hpCapacityW = 150_000.0;
    public boolean connected;
    public double co2CapturedKg;
    public double currentCaptureRateKgS;
}
