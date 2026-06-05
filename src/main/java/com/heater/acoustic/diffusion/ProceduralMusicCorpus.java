package com.heater.acoustic.diffusion;

import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.acoustic.FanNoiseSpectrum;

import java.util.Random;

/** Procedural ambient music + fan noise pairs for Java-LDM training. */
public final class ProceduralMusicCorpus {

    public record Pair(double[] fan, double[] music, String prompt) {}

    private ProceduralMusicCorpus() {}

    public static Pair[] generate(LatentDiffusionConfig cfg, AcousticSpectrumConfig specCfg) {
        Pair[] pairs = new Pair[cfg.corpusSize];
        Random rng = new Random(42);
        specCfg.clipDurationS = cfg.clipDurationS;
        specCfg.sampleRateHz = cfg.sampleRateHz;

        for (int i = 0; i < cfg.corpusSize; i++) {
            double[] fan = FanNoiseSpectrum.synthesizeWaveform(specCfg, rng);
            double[] music = synthesizeAmbient(cfg.sampleRateHz, cfg.clipDurationS, rng, i);
            pairs[i] = new Pair(fan, music, cfg.defaultPrompt);
        }
        return pairs;
    }

    static double[] synthesizeAmbient(int sr, double durationS, Random rng, int seed) {
        int n = (int) (sr * durationS);
        double[] x = new double[n];
        double[] freqs = {220, 277, 330, 392, 440, 554};
        double bpm = 60 + (seed % 20);
        double beat = 60.0 / bpm;

        for (int i = 0; i < n; i++) {
            double t = i / (double) sr;
            double env = 0.5 + 0.5 * Math.sin(2 * Math.PI * t / (beat * 4));
            for (int h = 0; h < freqs.length; h++) {
                double amp = 0.12 / (h + 1);
                x[i] += amp * env * Math.sin(2 * Math.PI * freqs[h] * t + rng.nextDouble());
            }
            x[i] += 0.02 * rng.nextGaussian();
        }
        normalize(x, 0.85);
        return x;
    }

    private static void normalize(double[] x, double peak) {
        double max = 0;
        for (double v : x) max = Math.max(max, Math.abs(v));
        if (max > 1e-12) {
            double s = peak / max;
            for (int i = 0; i < x.length; i++) x[i] *= s;
        }
    }
}
