package com.heater.model;

import com.heater.carbon.ConvectionCaptureCycle;

public final class PassiveConvectionLoad {
    public boolean enabled;
    public ConvectionCaptureCycle.Phase phase = ConvectionCaptureCycle.Phase.ADSORB;
    public double airflowM3S;
    public double fanBaselineW;
    public double fanResidualW;
    public double fanSavedW;
    public double deltaTK;
    public double co2CapturedKg;
    public double currentCaptureRateKgS;
    public double cycleTimeS;
}
