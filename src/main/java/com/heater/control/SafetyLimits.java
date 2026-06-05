package com.heater.control;

public record SafetyLimits(
        double primaryTTrip,
        double primaryTMax,
        double primaryMdotMin,
        double secondaryDtMax,
        double robotTaskTimeout
) {}
