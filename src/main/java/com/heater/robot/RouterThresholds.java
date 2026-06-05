package com.heater.robot;

public record RouterThresholds(
        double houseTempDelta,
        double poolTempDelta,
        double bufferChargeBelow,
        double algaeTempDelta
) {}
