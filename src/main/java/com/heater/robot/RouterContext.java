package com.heater.robot;

public record RouterContext(
        double simTime,
        double poolTemp,
        double poolSetpoint,
        double houseTemp,
        double houseSetpoint,
        double bufferTemp,
        double algaeTemp,
        double algaeOptimalTemp,
        double ambientTemp,
        boolean primarySafe,
        boolean ccsCanRun
) {}
