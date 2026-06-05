package com.heater.carbon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionCaptureCycleTest {

    @Test
    void adsorbPhaseDominatesCycleFraction() {
        ConvectionCaptureConfig cfg = referenceConfig();
        assertEquals(ConvectionCaptureCycle.Phase.ADSORB, ConvectionCaptureCycle.phaseAtTime(0, cfg));
        assertEquals(ConvectionCaptureCycle.Phase.ADSORB, ConvectionCaptureCycle.phaseAtTime(7 * 3600, cfg));
        assertEquals(ConvectionCaptureCycle.Phase.REGENERATE, ConvectionCaptureCycle.phaseAtTime(8.5 * 3600, cfg));
    }

    @Test
    void cycleProducesAnnualizedCapture() {
        ConvectionCaptureConfig cfg = referenceConfig();
        var draft = ConvectionCapturePhysics.solve(cfg, 34_000_000.0, 22.0);
        var cycle = ConvectionCaptureCycle.evaluate(cfg, draft, 45.0, 604_800.0);

        assertTrue(cycle.adsorbCaptureKgPerCycle() > 0);
        assertTrue(cycle.annualizedGrossTonnes() > 0);
        assertTrue(cycle.annualizedFanSavedMwh() > 0);
        assertTrue(cycle.avgAirflowM3S() > 0);
    }

    @Test
    void coldSourceReducesRegen() {
        ConvectionCaptureConfig cfg = referenceConfig();
        var draft = ConvectionCapturePhysics.solve(cfg, 34_000_000.0, 22.0);
        var warm = ConvectionCaptureCycle.evaluate(cfg, draft, 50.0, 604_800.0);
        var cold = ConvectionCaptureCycle.evaluate(cfg, draft, 30.0, 604_800.0);

        assertTrue(warm.regenHeatPumpKwhPerCycle() > cold.regenHeatPumpKwhPerCycle());
    }

    private static ConvectionCaptureConfig referenceConfig() {
        ConvectionCaptureConfig cfg = new ConvectionCaptureConfig();
        cfg.enabled = true;
        cfg.chimneyHeightM = 120.0;
        cfg.contactorAreaM2 = 50_000.0;
        cfg.bedResistancePaS2Kg2 = 0.002;
        cfg.wasteHeatToAirFraction = 0.35;
        cfg.captureEfficiency = 0.15;
        cfg.ambientCo2Ppm = 420.0;
        cfg.fanEfficiency = 0.65;
        cfg.minDeltaTK = 2.0;
        cfg.adsorbHours = 8.0;
        cfg.regenHours = 2.0;
        cfg.heatPumpCop = 3.2;
        cfg.hpCapacityW = 8_100_000.0;
        cfg.specificHeatDutyJPerKg = 5_500_000.0;
        cfg.minSourceTemp = 40.0;
        cfg.gridCo2KgPerKwh = 0.39;
        return cfg;
    }
}
