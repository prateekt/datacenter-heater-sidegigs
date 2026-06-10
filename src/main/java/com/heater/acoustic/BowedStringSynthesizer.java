package com.heater.acoustic;

import java.util.List;
import java.util.Random;

/** Statistical multi-voice bowed-string field for fan-orchestra WAV export and LDM targets. */
public final class BowedStringSynthesizer {

    private BowedStringSynthesizer() {}

    public static double[] synthesizeField(
            AcousticSpectrumConfig spectrum,
            FanOrchestraConfig orch,
            FanDrivenInstrumentField.InstrumentFieldResult field,
            Random rng
    ) {
        if (!orch.enabled || field.activeInstrumentCount() <= 0 || field.sampleVoices().isEmpty()) {
            return new double[(int) (spectrum.sampleRateHz * spectrum.clipDurationS)];
        }
        return synthesizeVoices(
                field.sampleVoices(),
                spectrum.bladePassingFrequencyHz() * orch.fanToBowGearRatio,
                spectrum.sampleRateHz,
                spectrum.clipDurationS,
                rng
        );
    }

    public static double[] synthesizeField(
            AcousticSpectrumConfig spectrum,
            FanOrchestraConfig orch,
            Random rng
    ) {
        FanNoiseSpectrum.SpectrumResult baseline = FanNoiseSpectrum.compute(spectrum);
        double[] afterAtten = ThirdOctaveBands.copy(baseline.lwPerBand());
        var field = FanDrivenInstrumentField.compute(
                spectrum, orch, afterAtten, baseline.bladePassingFrequencyHz());
        return synthesizeField(spectrum, orch, field, rng);
    }

    public static double[] synthesizeVoices(
            List<FanDrivenInstrumentField.VoiceSpec> voices,
            double bpfHz,
            int sampleRateHz,
            double durationS,
            Random rng
    ) {
        int n = (int) (sampleRateHz * durationS);
        double[] out = new double[n];
        if (voices.isEmpty()) return out;

        double scale = 1.0 / Math.sqrt(voices.size());
        for (FanDrivenInstrumentField.VoiceSpec voice : voices) {
            double phase = rng.nextDouble() * 2.0 * Math.PI;
            double amDepth = FanDrivenInstrumentField.coverAmDepth(voice.cover());
            double f0 = voice.fundamentalHz();

            for (int i = 0; i < n; i++) {
                double t = i / (double) sampleRateHz;
                double am = 1.0 + amDepth * Math.sin(2.0 * Math.PI * bpfHz * t);
                double bow = bowedWave(f0, t, phase, voice.cover());
                out[i] += bow * am * scale;
            }
        }
        normalize(out, 0.9);
        return out;
    }

    public static double harmonicity(double[] x, int sr, double fundamentalHz) {
        int period = Math.max(1, (int) Math.round(sr / fundamentalHz));
        if (period >= x.length) return 0;
        double corr = 0.0;
        double energy = 0.0;
        for (int i = period; i < x.length; i++) {
            corr += x[i] * x[i - period];
            energy += x[i] * x[i];
        }
        return Math.max(0.0, corr / Math.max(1e-30, energy));
    }

    /** Max lag-autocorrelation over pentatonic fundamentals (multi-voice field). */
    public static double peakHarmonicity(double[] x, int sr, FanOrchestraConfig orch) {
        double best = 0.0;
        int[] semitones = {0, 2, 4, 7, 9, 12, 14, 16, 19, 21};
        for (int sem : semitones) {
            if (sem > orch.semitoneSpread) break;
            double f0 = orch.fundamentalHz * Math.pow(2.0, sem / 12.0);
            best = Math.max(best, harmonicity(x, sr, f0));
        }
        return best;
    }

    /** Envelope AC/DC ratio at blade-passing rate — BPF tremolo depth proxy. */
    public static double bpfAmDepth(double[] x, int sr, double bpfHz) {
        if (x.length < 64 || bpfHz <= 0) return 0.0;
        int win = Math.max(8, (int) Math.round(sr / bpfHz / 4.0));
        int frames = Math.max(1, (x.length - win) / win);
        double[] env = new double[frames];
        for (int f = 0; f < frames; f++) {
            double sum = 0.0;
            int start = f * win;
            for (int i = start; i < start + win && i < x.length; i++) {
                sum += x[i] * x[i];
            }
            env[f] = Math.sqrt(sum / win);
        }
        double mean = 0.0;
        for (double v : env) mean += v;
        mean /= env.length;
        if (mean < 1e-12) return 0.0;
        double var = 0.0;
        for (double v : env) {
            double d = v - mean;
            var += d * d;
        }
        return Math.sqrt(var / env.length) / mean;
    }

    public static double tremoloSidebandRatio(double[] x, int sr, double bpfHz) {
        return bpfAmDepth(x, sr, bpfHz);
    }

    /** Max lag-autocorrelation over bow-fundamental periods (~4–20 ms). */
    public static double sustainIndex(double[] x, int sr) {
        double best = 0.0;
        for (int ms = 4; ms <= 20; ms++) {
            int lag = Math.max(1, sr * ms / 1000);
            if (lag >= x.length) continue;
            double corr = 0.0;
            double energy = 0.0;
            for (int i = lag; i < x.length; i++) {
                corr += x[i] * x[i - lag];
                energy += x[i] * x[i];
            }
            best = Math.max(best, corr / Math.max(1e-30, energy));
        }
        return Math.max(0.0, best);
    }

    private static double bowedWave(
            double f0,
            double t,
            double phase,
            FanDrivenInstrumentField.BowCover cover
    ) {
        double sum = 0.0;
        int partials = switch (cover) {
            case TREMOBOW -> 6;
            case VASE -> 4;
            case CURVED -> 5;
        };
        for (int h = 1; h <= partials; h++) {
            double amp = 1.0 / (h * (1.0 + 0.15 * h));
            if (cover == FanDrivenInstrumentField.BowCover.VASE && h > 2) {
                amp *= 0.6;
            }
            sum += amp * Math.sin(2.0 * Math.PI * f0 * h * t + phase / h);
        }
        return sum / partials;
    }

    private static void normalize(double[] x, double peak) {
        double max = 0.0;
        for (double v : x) max = Math.max(max, Math.abs(v));
        if (max > 1e-12) {
            double s = peak / max;
            for (int i = 0; i < x.length; i++) x[i] *= s;
        }
    }
}
