package com.heater.robot;

import com.heater.model.LoadTarget;
import com.heater.model.TaskType;

public final class RobotTask {
    public final TaskType taskType;
    public final LoadTarget target;
    public final double duration;
    public final LoadTarget fromTarget;
    public double elapsed;

    public RobotTask(TaskType taskType, LoadTarget target, double duration) {
        this(taskType, target, duration, LoadTarget.NONE);
    }

    public RobotTask(TaskType taskType, LoadTarget target, double duration, LoadTarget fromTarget) {
        this.taskType = taskType;
        this.target = target;
        this.duration = duration;
        this.fromTarget = fromTarget;
    }

    public boolean isComplete() {
        return elapsed >= duration;
    }

    public void tick(double dt) {
        elapsed += dt;
    }
}
