package com.heater.control;

public final class PidController {

    private final PidGains gains;
    private final double outputMin;
    private final double outputMax;
    private double setpoint;
    private double integral;
    private double prevError;

    public PidController(PidGains gains, double setpoint, double outputMin, double outputMax) {
        this.gains = gains;
        this.setpoint = setpoint;
        this.outputMin = outputMin;
        this.outputMax = outputMax;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    public double update(double measured, double dt) {
        if (dt <= 0) return outputMin;
        double error = setpoint - measured;
        integral += error * dt;
        double derivative = (error - prevError) / dt;
        prevError = error;
        double output = gains.kp() * error + gains.ki() * integral + gains.kd() * derivative;
        return clamp(output);
    }

    public double updateInverse(double measured, double dt) {
        if (dt <= 0) return outputMin;
        double error = measured - setpoint;
        integral += error * dt;
        double derivative = (error - prevError) / dt;
        prevError = error;
        double output = gains.kp() * error + gains.ki() * integral + gains.kd() * derivative;
        return clamp(output);
    }

    private double clamp(double output) {
        return Math.max(outputMin, Math.min(outputMax, output));
    }
}
