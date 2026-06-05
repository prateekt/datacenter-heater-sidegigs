package com.heater.carbon;

import com.heater.model.CarbonCaptureLoad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CarbonCaptureModelTest {

    @Test
    void stallsBelowMinSourceTemp() {
        CarbonCaptureLoad ccs = new CarbonCaptureLoad();
        ccs.connected = true;
        ccs.minSourceTemp = 40.0;

        var r = CarbonCaptureModel.integrate(ccs, 35.0, 5.0, true, 4186, 1.0);
        assertEquals(0, r.qRegenW(), 1e-6);
        assertEquals(0, ccs.co2CapturedKg, 1e-6);
    }

    @Test
    void capturesCo2WhenHotEnough() {
        CarbonCaptureLoad ccs = new CarbonCaptureLoad();
        ccs.connected = true;
        ccs.minSourceTemp = 40.0;
        ccs.hpCapacityW = 150_000;
        ccs.specificHeatDutyJPerKg = 5_500_000;

        CarbonCaptureModel.integrate(ccs, 50.0, 6.0, true, 4186, 3600);
        assertTrue(ccs.co2CapturedKg > 0);
        assertTrue(ccs.currentCaptureRateKgS > 0);
    }
}
