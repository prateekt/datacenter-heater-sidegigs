package com.heater.control;

import com.heater.model.ActuatorState;
import com.heater.model.LoadTarget;
import com.heater.model.SensorSnapshot;
import com.heater.model.SystemState;

public final class AutomationController {

    private final PidController rejectPid;
    private final PidController secondaryPid;
    private final double maxSecondaryFlow;
    private final SafetyLimits safetyLimits;

    public AutomationController(
            PidGains pidPrimary,
            PidGains pidSecondary,
            double primaryTSetpoint,
            double maxSecondaryFlow,
            SafetyLimits safetyLimits
    ) {
        this.rejectPid = new PidController(pidPrimary, primaryTSetpoint, 0.0, 1.0);
        this.secondaryPid = new PidController(pidSecondary, 0.0, 0.0, 1.0);
        this.maxSecondaryFlow = maxSecondaryFlow;
        this.safetyLimits = safetyLimits;
    }

    public ActuatorState step(
            SystemState state,
            SensorSnapshot sensors,
            LoadTarget target,
            double targetSetpoint,
            double dt,
            double robotTaskElapsed
    ) {
        secondaryPid.setSetpoint(targetSetpoint);

        SafetyBounds bounds = SafetyEvaluator.evaluate(sensors, safetyLimits, robotTaskElapsed);
        double reject = rejectPid.updateInverse(sensors.primaryTOut(), dt);

        double secondarySpeed;
        boolean poolValve;
        boolean houseValve;
        boolean ccsValve;
        boolean algaeValve;

        switch (target) {
            case NONE -> {
                secondarySpeed = 0.0;
                poolValve = false;
                houseValve = false;
                ccsValve = false;
                algaeValve = false;
                reject = Math.max(reject, 0.6);
            }
            case BUFFER -> {
                secondaryPid.setSetpoint(state.buffer.temperature + 5.0);
                secondarySpeed = secondaryPid.update(state.buffer.temperature, dt);
                poolValve = false;
                houseValve = false;
                ccsValve = false;
                algaeValve = false;
            }
            case POOL -> {
                secondaryPid.setSetpoint(state.pool.setpoint);
                secondarySpeed = secondaryPid.update(state.pool.temperature, dt);
                poolValve = true;
                houseValve = false;
                ccsValve = false;
                algaeValve = false;
            }
            case HOUSE -> {
                secondaryPid.setSetpoint(state.house.setpoint);
                secondarySpeed = secondaryPid.update(state.house.temperature, dt);
                poolValve = false;
                houseValve = true;
                ccsValve = false;
                algaeValve = false;
            }
            case CARBON_CAPTURE -> {
                secondaryPid.setSetpoint(state.carbonCapture.regenerationTemp);
                secondarySpeed = secondaryPid.update(state.buffer.temperature, dt);
                poolValve = false;
                houseValve = false;
                ccsValve = true;
                algaeValve = false;
            }
            case ALGAE -> {
                secondaryPid.setSetpoint(state.algae.optimalTemp);
                secondarySpeed = secondaryPid.update(state.algae.temperature, dt);
                poolValve = false;
                houseValve = false;
                ccsValve = false;
                algaeValve = true;
            }
            default -> {
                secondarySpeed = 0.0;
                poolValve = false;
                houseValve = false;
                ccsValve = false;
                algaeValve = false;
            }
        }

        SafetyEvaluator.ActuatorClamp clamped = SafetyEvaluator.clamp(
                reject, secondarySpeed, poolValve, houseValve, ccsValve, algaeValve, bounds
        );

        if (bounds.forceFullReject && !anyLoadConnected(state)) {
            clamped = new SafetyEvaluator.ActuatorClamp(
                    Math.max(clamped.rejectFraction(), bounds.rejectFractionMin),
                    clamped.secondaryPumpSpeed(),
                    clamped.poolValve(),
                    clamped.houseValve(),
                    clamped.ccsValve(),
                    clamped.algaeValve()
            );
        }

        ActuatorState actuators = new ActuatorState();
        actuators.rejectFraction = clamped.rejectFraction();
        actuators.secondaryPumpSpeed = clamped.secondaryPumpSpeed();
        actuators.poolValveOpen = clamped.poolValve();
        actuators.houseValveOpen = clamped.houseValve();
        actuators.ccsValveOpen = clamped.ccsValve();
        actuators.algaeValveOpen = clamped.algaeValve();
        actuators.secondaryFlowKgS = clamped.secondaryPumpSpeed() * maxSecondaryFlow;
        return actuators;
    }

    private static boolean anyLoadConnected(SystemState state) {
        return state.pool.connected || state.house.connected
                || state.carbonCapture.connected || state.algae.connected;
    }
}
