package com.heater.carbon;

import com.heater.model.AlgaeLoad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlgaeGrowthModelTest {

    @Test
    void zeroGrowthAtNight() {
        AlgaeLoad algae = new AlgaeLoad();
        algae.temperature = 28.0;
        double rate = AlgaeGrowthModel.integrateGrowth(algae, 0.0, 0.0, 3600);
        assertEquals(0, rate, 1e-9);
        assertEquals(0, algae.co2FixedKg, 1e-9);
    }

    @Test
    void growthAtOptimalTempMidday() {
        AlgaeLoad algae = new AlgaeLoad();
        algae.connected = true;
        algae.temperature = 28.0;
        algae.optimalTemp = 28.0;
        algae.surfaceAreaM2 = 500;
        double noon = 12 * 3600;
        AlgaeGrowthModel.integrateGrowth(algae, noon, 0.0, 3600);
        assertTrue(algae.co2FixedKg > 0);
    }

    @Test
    void dacEnrichmentBoostsGrowth() {
        AlgaeLoad algae = new AlgaeLoad();
        algae.connected = true;
        algae.temperature = 28.0;
        algae.optimalTemp = 28.0;
        double noon = 12 * 3600;
        AlgaeGrowthModel.integrateGrowth(algae, noon, 0.0, 1.0);
        double without = algae.co2FixedKg;
        algae.co2FixedKg = 0;
        AlgaeGrowthModel.integrateGrowth(algae, noon, 0.05, 1.0);
        assertTrue(algae.co2FixedKg > without);
    }
}
