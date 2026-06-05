package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MechanicalDiffusionPhysicsV2Test {

    @Test
    void v2SpectralDistanceLowerThanV1() {
        MechanicalDiffusionConfig v1Cfg = new MechanicalDiffusionConfig();
        v1Cfg.reverseSteps = 20;
        v1Cfg.sampleRateHz = 44100;
        v1Cfg.clipDurationS = 1.0;

        MdmgLandscapeConfig v2Cfg = new MdmgLandscapeConfig();
        v2Cfg.reverseSteps = 20;

        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.sampleRateHz = 44100;
        spec.clipDurationS = 1.0;
        Random rng = new Random(3);
        double[] fan = FanNoiseSpectrum.synthesizeWaveform(spec, rng);

        var v1 = MechanicalDiffusionPhysics.denoise(v1Cfg, fan, rng);
        var v2 = MechanicalDiffusionPhysicsV2.denoise(fan, v2Cfg, rng);

        assertTrue(v2.spectralDistance() < v1.spectralDistanceToTemplate(),
                "v2 STFT Langevin should beat v1 template distance: v2="
                        + v2.spectralDistance() + " v1=" + v1.spectralDistanceToTemplate());
    }

    @Test
    void v2ProducesStableOutput() {
        MdmgLandscapeConfig cfg = new MdmgLandscapeConfig();
        cfg.diagonalK = new double[] {0.05, 0.06, 0.07, 0.08};
        cfg.reverseSteps = 10;
        double[] fan = new double[4096];
        Random rng = new Random(1);
        for (int i = 0; i < fan.length; i++) fan[i] = rng.nextGaussian() * 0.1;

        var result = MechanicalDiffusionPhysicsV2.denoise(fan, cfg, rng);
        assertEquals(fan.length, result.outputWaveform().length);
        for (double v : result.outputWaveform()) {
            assertFalse(Double.isNaN(v));
            assertTrue(Math.abs(v) <= 1.0);
        }
    }
}
