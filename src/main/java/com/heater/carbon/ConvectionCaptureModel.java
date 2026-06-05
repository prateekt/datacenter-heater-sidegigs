package com.heater.carbon;

import com.heater.model.ActuatorState;
import com.heater.model.CarbonCaptureLoad;
import com.heater.model.PassiveConvectionLoad;
import com.heater.model.SystemState;

public final class ConvectionCaptureModel {

    private ConvectionCaptureModel() {}

    public record IntegrationResult(
            double captureRateKgS,
            double fanElectricW,
            double regenElectricW,
            double qRegenW
    ) {}

    public static IntegrationResult integrate(
            SystemState state,
            ConvectionCaptureConfig cfg,
            double qRejectW,
            ActuatorState actuators,
            double cpWater,
            double dt
    ) {
        PassiveConvectionLoad pc = state.passiveConvection;
        if (!cfg.convectionHybrid() || !pc.enabled) {
            pc.currentCaptureRateKgS = 0.0;
            return new IntegrationResult(0, 0, 0, 0);
        }

        pc.phase = ConvectionCaptureCycle.phaseAtTime(pc.cycleTimeS, cfg);
        pc.cycleTimeS += dt;

        if (pc.phase == ConvectionCaptureCycle.Phase.ADSORB) {
            return integrateAdsorb(state, cfg, qRejectW, dt);
        }
        return integrateRegen(state, cfg, actuators, cpWater, dt);
    }

    private static IntegrationResult integrateAdsorb(
            SystemState state,
            ConvectionCaptureConfig cfg,
            double qRejectW,
            double dt
    ) {
        PassiveConvectionLoad pc = state.passiveConvection;
        double qAir = cfg.wasteHeatToAirFraction * qRejectW;
        var draft = ConvectionCapturePhysics.solveFromAirHeat(cfg, qAir, state.ambientTemp);

        pc.airflowM3S = draft.volumeFlowM3S();
        pc.fanBaselineW = draft.fanBaselineW();
        pc.fanResidualW = draft.fanResidualW();
        pc.fanSavedW = draft.fanSavedW();
        pc.deltaTK = draft.deltaTK();
        pc.currentCaptureRateKgS = draft.captureRateKgS();
        pc.co2CapturedKg += pc.currentCaptureRateKgS * dt;
        state.carbonCapture.co2CapturedKg += pc.currentCaptureRateKgS * dt;
        state.convectionCo2CapturedKg = pc.co2CapturedKg;

        state.fanElectricJ += draft.fanResidualW() * dt;
        return new IntegrationResult(draft.captureRateKgS(), draft.fanResidualW(), 0, 0);
    }

    private static IntegrationResult integrateRegen(
            SystemState state,
            ConvectionCaptureConfig cfg,
            ActuatorState actuators,
            double cpWater,
            double dt
    ) {
        PassiveConvectionLoad pc = state.passiveConvection;
        pc.airflowM3S = 0.0;
        pc.currentCaptureRateKgS = 0.0;

        CarbonCaptureLoad ccs = state.carbonCapture;
        CarbonCaptureModel.CaptureResult capture = CarbonCaptureModel.integrate(
                ccs,
                state.buffer.temperature,
                actuators.secondaryFlowKgS,
                actuators.ccsValveOpen,
                cpWater,
                dt
        );
        CarbonCaptureModel.accumulateElectric(state, capture.electricW(), dt);
        return new IntegrationResult(
                0,
                0,
                capture.electricW(),
                capture.qRegenW()
        );
    }
}
