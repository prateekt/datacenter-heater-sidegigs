package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StftEngineTest {

    @Test
    void roundTripPreservesEnergyApproximately() {
        int sr = 8000;
        int n = 4096;
        double[] original = new double[n];
        for (int i = 0; i < n; i++) {
            original[i] = 0.5 * Math.sin(2 * Math.PI * 440 * i / sr);
        }
        StftEngine.StftResult stft = StftEngine.forward(original, sr);
        double[] reconstructed = StftEngine.inverse(stft, n);
        double err = 0;
        for (int i = 0; i < n; i++) {
            err += Math.abs(original[i] - reconstructed[i]);
        }
        assertTrue(err / n < 0.15, "STFT round-trip error too high: " + err / n);
    }
}
