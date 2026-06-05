package com.heater.acoustic.diffusion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiffusionSchedulerTest {

    @Test
    void alphaBarsAreMonotonic() {
        DiffusionScheduler s = new DiffusionScheduler(20, 0.0001, 0.02);
        for (int i = 1; i < s.steps; i++) {
            assertTrue(s.alphaBars[i] <= s.alphaBars[i - 1] + 1e-9);
        }
    }

    @Test
    void addNoiseChangesState() {
        DiffusionScheduler s = new DiffusionScheduler(10, 0.001, 0.1);
        double[] x0 = {1.0, 0.5, -0.3};
        double[] xt = s.addNoise(x0, 5, new java.util.Random(0));
        assertNotEquals(x0[0], xt[0], 1e-6);
    }
}
