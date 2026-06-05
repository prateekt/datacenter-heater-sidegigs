package com.heater.robot;

import com.heater.model.LoadTarget;

public record RobotTaskLog(double time, String event, LoadTarget target, String detail) {
    public RobotTaskLog(double time, String event, LoadTarget target) {
        this(time, event, target, "");
    }
}
