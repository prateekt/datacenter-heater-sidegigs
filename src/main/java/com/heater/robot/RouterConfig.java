package com.heater.robot;

import com.heater.model.LoadTarget;

import java.util.List;

public final class RouterConfig {
    public double connectDuration = 15.0;
    public double switchDuration = 30.0;
    public double houseOutsideThreshold = 10.0;
    public double decisionInterval = 30.0;
    public List<LoadTarget> priority = List.of(
            LoadTarget.HOUSE, LoadTarget.CARBON_CAPTURE, LoadTarget.ALGAE, LoadTarget.POOL, LoadTarget.BUFFER
    );
    public RouterThresholds thresholds = new RouterThresholds(1.0, 0.5, 45.0, 1.0);
}
