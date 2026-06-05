package com.heater.thermal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeatExchangerTest {

    @Test
    void zeroSecondaryFlowReturnsNoTransfer() {
        var r = HeatExchangerCalc.compute(60, 20, 10, 0, 50_000, 4186);
        assertEquals(0, r.qTransferW(), 1e-6);
        assertEquals(60, r.tPrimaryOut(), 1e-6);
    }

    @Test
    void transfersHeatWhenPrimaryHotter() {
        var r = HeatExchangerCalc.compute(60, 20, 10, 8, 80_000, 4186);
        assertTrue(r.qTransferW() > 0);
        assertTrue(r.tPrimaryOut() < 60);
        assertTrue(r.tSecondaryOut() > 20);
    }

    @Test
    void energyBalance() {
        double mdotP = 12, mdotS = 6, cp = 4186;
        double tPIn = 55, tSIn = 25;
        var r = HeatExchangerCalc.compute(tPIn, tSIn, mdotP, mdotS, 80_000, cp);
        double qPrimary = mdotP * cp * (tPIn - r.tPrimaryOut());
        double qSecondary = mdotS * cp * (r.tSecondaryOut() - tSIn);
        assertEquals(qPrimary, r.qTransferW(), 1.0);
        assertEquals(qSecondary, r.qTransferW(), 1.0);
    }
}
