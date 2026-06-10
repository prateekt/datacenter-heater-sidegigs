package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FanDrivenInstrumentFieldTest {

    @Test
    void voiceCountScalesWithFanCount() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.fansPerRack = 6;
        spec.rackCount = 2500;
        FanOrchestraConfig orch = new FanOrchestraConfig();
        orch.enabled = true;
        orch.instrumentsPerFan = 1.0;
        orch.instrumentedFraction = 1.0;

        assertEquals(15_000, FanDrivenInstrumentField.activeInstrumentCount(spec, orch));

        orch.instrumentedFraction = 0.5;
        assertEquals(7_500, FanDrivenInstrumentField.activeInstrumentCount(spec, orch));
    }

    @Test
    void musicalContentIncreasesWithInstrumentedFraction() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.rackCount = 2500;
        spec.fansPerRack = 6;
        FanNoiseSpectrum.SpectrumResult baseline = FanNoiseSpectrum.compute(spec);
        double bpf = baseline.bladePassingFrequencyHz();
        double[] afterAtten = ThirdOctaveBands.copy(baseline.lwPerBand());

        FanOrchestraConfig low = new FanOrchestraConfig();
        low.instrumentedFraction = 0.1;
        FanOrchestraConfig high = new FanOrchestraConfig();
        high.instrumentedFraction = 1.0;

        var lowResult = FanDrivenInstrumentField.compute(spec, low, afterAtten, bpf);
        var highResult = FanDrivenInstrumentField.compute(spec, high, afterAtten, bpf);

        assertTrue(highResult.musicalContentDb() > lowResult.musicalContentDb());
        assertTrue(highResult.activeInstrumentCount() > lowResult.activeInstrumentCount());
    }

    @Test
    void coverPresetsProduceSampleVoices() {
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        FanOrchestraConfig orch = new FanOrchestraConfig();
        orch.bowCoverCycle = "tremobow,vase,curved";
        var field = FanDrivenInstrumentField.compute(
                spec, orch, ThirdOctaveBands.copy(new double[ThirdOctaveBands.bandCount()]), 490.0);
        assertFalse(field.sampleVoices().isEmpty());
        assertTrue(field.sampleVoices().stream().anyMatch(v -> v.cover() == FanDrivenInstrumentField.BowCover.VASE));
    }
}
