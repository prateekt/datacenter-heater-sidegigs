package com.heater.acoustic;

import java.util.ArrayList;
import java.util.List;

/**
 * Hall-scale fan orchestra — one rotary-bow string cell per cooling fan (aggregate SPL).
 * Inspired by Pickaso Rotary Bow: BPF-driven tremolo, interchangeable wheel covers.
 */
public final class FanDrivenInstrumentField {

    public enum BowCover {
        TREMOBOW, VASE, CURVED;

        static BowCover fromCycle(String[] covers, int voiceId) {
            if (covers.length == 0) return TREMOBOW;
            String raw = covers[voiceId % covers.length].trim().toUpperCase();
            try {
                return valueOf(raw);
            } catch (IllegalArgumentException e) {
                return TREMOBOW;
            }
        }
    }

    public record VoiceSpec(int voiceId, double fundamentalHz, BowCover cover) {}

    public record InstrumentFieldResult(
            double[] lwPerBand,
            int activeInstrumentCount,
            double musicalContentDb,
            double sustainDb,
            double tremoloDepthDb,
            List<VoiceSpec> sampleVoices
    ) {}

    private static final int[] PENTATONIC_SEMITONES = {0, 2, 4, 7, 9, 12, 14, 16, 19, 21};

    private FanDrivenInstrumentField() {}

    public static InstrumentFieldResult compute(
            AcousticSpectrumConfig spectrum,
            FanOrchestraConfig orch,
            double[] sourceLwAfterAtten,
            double bpfHz
    ) {
        int n = ThirdOctaveBands.bandCount();
        double[] lw = new double[n];
        List<VoiceSpec> emptyVoices = List.of();

        if (!orch.enabled) {
            return new InstrumentFieldResult(lw, 0, -200, 0, 0, emptyVoices);
        }

        int voiceCount = (int) Math.round(
                spectrum.totalFanCount() * orch.instrumentsPerFan * orch.instrumentedFraction);
        voiceCount = Math.max(0, voiceCount);
        if (voiceCount == 0) {
            return new InstrumentFieldResult(lw, 0, -200, 0, 0, emptyVoices);
        }

        String[] covers = orch.bowCoverCycle.split(",");
        double voiceCountDb = 10.0 * Math.log10(Math.max(1, voiceCount));
        double perVoiceBase = 28.0;

        int tuningGroups = Math.min(PENTATONIC_SEMITONES.length, Math.max(1, orch.semitoneSpread / 3));
        for (int g = 0; g < tuningGroups; g++) {
            int semitones = PENTATONIC_SEMITONES[g % PENTATONIC_SEMITONES.length];
            if (semitones > orch.semitoneSpread) {
                semitones = semitones % orch.semitoneSpread;
            }
            double f0 = orch.fundamentalHz * Math.pow(2.0, semitones / 12.0);
            BowCover cover = BowCover.fromCycle(covers, g);
            double coverBoost = coverBrightnessDb(cover);

            for (int partial = 1; partial <= 4; partial++) {
                double freq = f0 * partial;
                if (freq > 7500) break;
                int band = ThirdOctaveBands.nearestBandIndex(freq);
                double partialDb = perVoiceBase + coverBoost - 6.0 * (partial - 1)
                        + voiceCountDb - 10.0 * Math.log10(tuningGroups);
                lw[band] = logAdd(lw[band], partialDb);
            }
        }

        double reallocated = reallocateBpfEnergy(sourceLwAfterAtten, bpfHz, orch.bpfEnergyReallocation, voiceCount);
        for (int i = 0; i < n; i++) {
            if (ThirdOctaveBands.CENTER_HZ[i] >= 200 && ThirdOctaveBands.CENTER_HZ[i] <= 2000) {
                lw[i] = logAdd(lw[i], reallocated);
            }
        }

        double musicalPower = bandPower(lw, 200, 2000);
        double musicalContentDb = 10.0 * Math.log10(Math.max(1e-30, musicalPower));
        double tremoloDepthDb = 6.0 + 4.0 * orch.fanToBowGearRatio;
        double sustainDb = 12.0 + voiceCountDb * 0.15;

        List<VoiceSpec> sampleVoices = buildSampleVoices(
                voiceCount, orch, covers, orch.statisticalVoiceSample);

        return new InstrumentFieldResult(
                lw, voiceCount, musicalContentDb, sustainDb, tremoloDepthDb, sampleVoices);
    }

    public static int activeInstrumentCount(AcousticSpectrumConfig spectrum, FanOrchestraConfig orch) {
        if (!orch.enabled) return 0;
        return (int) Math.round(
                spectrum.totalFanCount() * orch.instrumentsPerFan * orch.instrumentedFraction);
    }

    static List<VoiceSpec> buildSampleVoices(
            int voiceCount,
            FanOrchestraConfig orch,
            String[] covers,
            int sampleSize
    ) {
        int n = Math.min(sampleSize, Math.max(1, voiceCount));
        List<VoiceSpec> voices = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int voiceId = (int) ((long) i * voiceCount / n);
            int semitones = PENTATONIC_SEMITONES[voiceId % PENTATONIC_SEMITONES.length];
            semitones = semitones % Math.max(1, orch.semitoneSpread);
            double f0 = orch.fundamentalHz * Math.pow(2.0, semitones / 12.0);
            voices.add(new VoiceSpec(voiceId, f0, BowCover.fromCycle(covers, voiceId)));
        }
        return voices;
    }

    static double coverBrightnessDb(BowCover cover) {
        return switch (cover) {
            case TREMOBOW -> 2.0;
            case VASE -> -1.0;
            case CURVED -> 0.5;
        };
    }

    static double coverAmDepth(BowCover cover) {
        return switch (cover) {
            case TREMOBOW -> 0.45;
            case VASE -> 0.25;
            case CURVED -> 0.35;
        };
    }

    private static double reallocateBpfEnergy(
            double[] sourceLw,
            double bpfHz,
            double fraction,
            int voiceCount
    ) {
        double captured = 0.0;
        for (int h = 1; h <= 3; h++) {
            int band = ThirdOctaveBands.nearestBandIndex(bpfHz * h);
            if (band < sourceLw.length) {
                captured += Math.pow(10.0, sourceLw[band] / 10.0) * fraction;
            }
        }
        if (voiceCount > 0) {
            captured *= Math.min(1.0, voiceCount / 1000.0);
        }
        return captured > 0 ? 10.0 * Math.log10(captured) - 10.0 * Math.log10(Math.max(1, voiceCount / 50.0)) : -200;
    }

    private static double bandPower(double[] lw, double fLo, double fHi) {
        double sum = 0.0;
        for (int i = 0; i < lw.length; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            if (f >= fLo && f <= fHi && lw[i] > -150) {
                sum += Math.pow(10.0, lw[i] / 10.0);
            }
        }
        return sum;
    }

    private static double logAdd(double a, double b) {
        if (a <= -200) return b;
        if (b <= -200) return a;
        return 10.0 * Math.log10(Math.pow(10.0, a / 10.0) + Math.pow(10.0, b / 10.0));
    }
}
