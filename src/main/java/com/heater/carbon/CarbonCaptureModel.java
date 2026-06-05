package com.heater.carbon;

import com.heater.model.ActuatorState;
import com.heater.model.CarbonCaptureLoad;
import com.heater.model.SystemState;

public final class CarbonCaptureModel {

    private CarbonCaptureModel() {}

    public record CaptureResult(double qRegenW, double qSourceDrawW, double electricW) {}

    public static CaptureResult integrate(
            CarbonCaptureLoad ccs,
            double sourceTemp,
            double secondaryFlowKgS,
            boolean valveOpen,
            double cpWater,
            double dt
    ) {
        ccs.currentCaptureRateKgS = 0.0;
        if (!ccs.connected || !valveOpen || secondaryFlowKgS <= 0 || sourceTemp < ccs.minSourceTemp) {
            return new CaptureResult(0.0, 0.0, 0.0);
        }

        double qAvailable = secondaryFlowKgS * cpWater * Math.max(0.0, sourceTemp - ccs.minSourceTemp);
        double qRegen = Math.min(qAvailable, ccs.hpCapacityW);
        double qSourceDraw = qRegen;
        double electricW = ccs.heatPumpCop > 0 ? qRegen / ccs.heatPumpCop : qRegen;

        double captureRate = ccs.specificHeatDutyJPerKg > 0 ? qRegen / ccs.specificHeatDutyJPerKg : 0.0;
        ccs.currentCaptureRateKgS = captureRate;
        ccs.co2CapturedKg += captureRate * dt;

        return new CaptureResult(qRegen, qSourceDraw, electricW);
    }

    public static void accumulateElectric(SystemState state, double electricW, double dt) {
        state.heatPumpElectricJ += electricW * dt;
    }
}
