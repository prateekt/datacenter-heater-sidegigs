package com.heater.carbon;

import com.heater.model.SystemState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClimateImpactCalculatorTest {

    @Test
    void netEqualsGrossMinusPenalty() {
        SystemState s = new SystemState();
        s.carbonCapture.co2CapturedKg = 50;
        s.algae.co2FixedKg = 30;
        s.heatPumpElectricJ = 10_000_000; // ~2.78 kWh

        ClimateImpactCalculator calc = new ClimateImpactCalculator(0.4, 0.8);
        var r = calc.report(s, 86400);
        assertEquals(80, r.grossRemovalKg(), 1e-6);
        assertEquals(80 - r.operationalPenaltyKg(), r.netCo2eRemovedKg(), 1e-6);
    }

    @Test
    void annualizationScalesCorrectly() {
        SystemState s = new SystemState();
        s.carbonCapture.co2CapturedKg = 10;
        ClimateImpactCalculator calc = new ClimateImpactCalculator(0.4, 0.8);
        var r = calc.report(s, 86400);
        assertTrue(r.annualizedTonnesCo2e() > 0);
    }
}
