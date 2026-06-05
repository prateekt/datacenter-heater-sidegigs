package com.heater.thermal;

import com.heater.carbon.ConvectionCaptureConfig;

public final class PhysicsConfig {
    public double cpWater = 4186.0;
    public double uaHx = 80_000.0;
    public double rejectCapacity = 500_000.0;
    public double primaryTMax = 65.0;
    public ConvectionCaptureConfig convection = ConvectionCaptureConfig.disabled();
}
