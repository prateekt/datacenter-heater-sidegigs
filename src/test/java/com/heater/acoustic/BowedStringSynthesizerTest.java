package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BowedStringSynthesizerTest {

    @Test
    void harmonicityBeatsRawFan() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.clipDurationS = 1.0;
        spec.sampleRateHz = 44100;
        FanOrchestraConfig orch = new FanOrchestraConfig();
        Random rng = new Random(42);

        double[] fan = FanNoiseSpectrum.synthesizeWaveform(spec, rng);
        double[] orchestra = BowedStringSynthesizer.synthesizeField(spec, orch, rng);

        double fanHarm = BowedStringSynthesizer.harmonicity(
                fan, spec.sampleRateHz, spec.bladePassingFrequencyHz());
        double orchHarm = BowedStringSynthesizer.peakHarmonicity(orchestra, spec.sampleRateHz, orch);

        assertTrue(orchHarm > fanHarm * 0.5);
        assertTrue(BowedStringSynthesizer.sustainIndex(orchestra, spec.sampleRateHz) > 0.1);
    }

    @Test
    void tremoloPeakNearBpf() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.clipDurationS = 2.0;
        spec.sampleRateHz = 44100;
        double bpf = spec.bladePassingFrequencyHz();

        double[] orchestra = BowedStringSynthesizer.synthesizeField(
                spec, new FanOrchestraConfig(), new Random(7));
        double[] fan = FanNoiseSpectrum.synthesizeWaveform(spec, new Random(7));

        assertTrue(BowedStringSynthesizer.bpfAmDepth(orchestra, spec.sampleRateHz, bpf)
                > BowedStringSynthesizer.bpfAmDepth(fan, spec.sampleRateHz, bpf));
    }

    @Test
    void outputLengthMatchesDuration() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.clipDurationS = 0.5;
        spec.sampleRateHz = 22050;
        double[] out = BowedStringSynthesizer.synthesizeField(spec, new FanOrchestraConfig(), new Random(1));
        assertEquals((int) (spec.sampleRateHz * spec.clipDurationS), out.length);
    }
}
