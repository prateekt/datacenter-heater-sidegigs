package com.heater.thermal;

import com.heater.carbon.AlgaeGrowthModel;
import com.heater.carbon.CarbonCaptureModel;
import com.heater.model.ActuatorState;
import com.heater.model.SystemState;

public final class PhysicsEngine {

    private final PhysicsConfig cfg;

    public PhysicsEngine(PhysicsConfig cfg) {
        this.cfg = cfg;
    }

    public void applyHxAndLoads(SystemState state, ActuatorState actuators, double dt) {
        double cp = cfg.cpWater;

        double rejectFrac = Math.max(0.0, Math.min(1.0, actuators.rejectFraction));
        double qReject = Math.min(rejectFrac * cfg.rejectCapacity, state.primary.qWaste);

        double tPrimaryPreHx = state.primary.tIn;
        if (state.primary.mdot > 0) {
            tPrimaryPreHx = state.primary.tIn + state.primary.qWaste / (state.primary.mdot * cp);
        }

        double mdotSec = actuators.secondaryFlowKgS;
        HeatExchangerCalc.HxResult hx = HeatExchangerCalc.compute(
                tPrimaryPreHx,
                state.buffer.temperature,
                state.primary.mdot,
                mdotSec,
                cfg.uaHx,
                cp
        );

        state.primary.tOut = hx.tPrimaryOut();
        state.hx.qTransfer = hx.qTransferW();
        state.hx.tPrimaryOutHx = hx.tPrimaryOut();
        state.hx.tSecondaryOut = hx.tSecondaryOut();

        updateBuffer(state, hx.qTransferW(), mdotSec, cp, dt);
        updatePool(state, actuators, cp, dt);
        updateHouse(state, actuators, cp, dt);
        AlgaeGrowthModel.updateThermal(state.algae, state.ambientTemp, actuators, cp, dt);

        double dacRateBefore = state.carbonCapture.currentCaptureRateKgS;
        CarbonCaptureModel.CaptureResult capture = CarbonCaptureModel.integrate(
                state.carbonCapture,
                state.buffer.temperature,
                mdotSec,
                actuators.ccsValveOpen,
                cp,
                dt
        );
        CarbonCaptureModel.accumulateElectric(state, capture.electricW(), dt);

        AlgaeGrowthModel.integrateGrowth(
                state.algae,
                state.time,
                dacRateBefore > 0 ? dacRateBefore : state.carbonCapture.currentCaptureRateKgS,
                dt
        );

        double qDelivered = 0.0;
        if (state.pool.connected && actuators.poolValveOpen && mdotSec > 0) {
            qDelivered += hx.qTransferW() * (state.house.connected ? 0.5 : 0.7);
        }
        if (state.house.connected && actuators.houseValveOpen && mdotSec > 0) {
            qDelivered += hx.qTransferW() * (state.pool.connected ? 0.5 : 0.7);
        }
        if (state.carbonCapture.connected && actuators.ccsValveOpen && mdotSec > 0) {
            qDelivered += capture.qSourceDrawW();
        }
        if (state.algae.connected && actuators.algaeValveOpen && mdotSec > 0) {
            qDelivered += mdotSec * 0.35 * cp * Math.max(0.0, state.algae.optimalTemp - state.algae.temperature);
        }
        qDelivered = Math.min(qDelivered, hx.qTransferW() + capture.qSourceDrawW());

        state.energyRecoveredJ += qDelivered * dt;
        state.energyRejectedJ += qReject * dt;

        if (state.primary.tOut > cfg.primaryTMax) {
            state.primaryUnsafeTimeS += dt;
        }
        if ((state.carbonCapture.connected && state.carbonCapture.currentCaptureRateKgS > 0)
                || (state.algae.connected && state.algae.currentGrowthRateKgS > 0)) {
            state.ccsActiveSteps++;
        }
    }

    private void updateBuffer(SystemState state, double qIn, double mdot, double cp, double dt) {
        double mass = state.buffer.volume;
        if (mass <= 0) return;
        double energy = qIn * dt;
        if (mdot > 0) {
            double tMix = state.buffer.temperature + qIn / (mdot * cp);
            state.buffer.temperature += energy / (mass * cp);
            state.buffer.temperature = 0.5 * state.buffer.temperature + 0.5 * tMix;
        } else {
            state.buffer.temperature += energy / (mass * cp);
        }
    }

    private void updatePool(SystemState state, ActuatorState actuators, double cp, double dt) {
        var pool = state.pool;
        double qLoss = pool.lossUa * (pool.temperature - state.ambientTemp);
        double qGain = 0.0;
        if (pool.connected && actuators.poolValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.3 * cp
                    * Math.max(0.0, pool.setpoint - pool.temperature);
        }
        pool.temperature += (qGain - qLoss) * dt / (pool.volume * cp);
    }

    private void updateHouse(SystemState state, ActuatorState actuators, double cp, double dt) {
        var house = state.house;
        double qLoss = house.lossUa * (house.temperature - state.ambientTemp);
        double qGain = 0.0;
        if (house.connected && actuators.houseValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.4 * cp
                    * Math.max(0.0, house.setpoint - house.temperature);
        }
        double thermalMassJK = house.thermalMass * 1000.0;
        house.temperature += (qGain - qLoss) * dt / thermalMassJK;
    }
}
