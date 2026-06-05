package com.heater.carbon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionCapturePhysicsTest {

    @Test
    void tallChimneyProducesMeaningfulDraft() {
        ConvectionCaptureConfig cfg = referenceConfig();
        cfg.chimneyHeightM = 100.0;
        double qWaste = 34_000_000.0;

        var draft = ConvectionCapturePhysics.solve(cfg, qWaste, 22.0);

        assertTrue(draft.volumeFlowM3S() > 10.0, "expected meaningful natural draft");
        assertTrue(draft.deltaTK() >= cfg.minDeltaTK);
        assertTrue(draft.captureRateKgS() > 0);
        assertTrue(draft.fanSavedW() > 0);
    }

    @Test
    void tallerChimneyIncreasesAirflow() {
        ConvectionCaptureConfig low = referenceConfig();
        low.chimneyHeightM = 40.0;
        ConvectionCaptureConfig high = referenceConfig();
        high.chimneyHeightM = 160.0;
        double qWaste = 34_000_000.0;

        var lowDraft = ConvectionCapturePhysics.solve(low, qWaste, 22.0);
        var highDraft = ConvectionCapturePhysics.solve(high, qWaste, 22.0);

        assertTrue(highDraft.volumeFlowM3S() > lowDraft.volumeFlowM3S());
        assertTrue(highDraft.fanSavedW() > lowDraft.fanSavedW());
    }

    @Test
    void largerContactorAreaIncreasesCaptureAndAirflow() {
        ConvectionCaptureConfig small = referenceConfig();
        small.contactorAreaM2 = 10_000.0;
        ConvectionCaptureConfig large = referenceConfig();
        large.contactorAreaM2 = 100_000.0;
        double qWaste = 34_000_000.0;

        var smallDraft = ConvectionCapturePhysics.solve(small, qWaste, 22.0);
        var largeDraft = ConvectionCapturePhysics.solve(large, qWaste, 22.0);

        assertTrue(largeDraft.captureRateKgS() > smallDraft.captureRateKgS() * 2.0,
                "capture should scale well above 2× when contactor area grows 10×");
        assertTrue(largeDraft.volumeFlowM3S() > smallDraft.volumeFlowM3S(),
                "wider bed should lower resistance and increase draft");
    }

    @Test
    void zeroWasteHeatProducesNoDraft() {
        ConvectionCaptureConfig cfg = referenceConfig();
        var draft = ConvectionCapturePhysics.solve(cfg, 0, 22.0);
        assertEquals(0, draft.massFlowKgS(), 1e-6);
        assertEquals(0, draft.captureRateKgS(), 1e-9);
    }

    private static ConvectionCaptureConfig referenceConfig() {
        ConvectionCaptureConfig cfg = new ConvectionCaptureConfig();
        cfg.enabled = true;
        cfg.chimneyHeightM = 120.0;
        cfg.chimneyAreaM2 = 800.0;
        cfg.contactorAreaM2 = 50_000.0;
        cfg.bedResistancePaS2Kg2 = 0.002;
        cfg.wasteHeatToAirFraction = 0.35;
        cfg.captureEfficiency = 0.15;
        cfg.ambientCo2Ppm = 420.0;
        cfg.fanEfficiency = 0.65;
        cfg.minDeltaTK = 2.0;
        return cfg;
    }

}
