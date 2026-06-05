package com.heater.acoustic.diffusion;

import com.heater.acoustic.StftEngine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/** Fan noise + prompt → music via Java latent diffusion inference. */
public final class LatentDiffusionInference {

    public record InferenceResult(
            double[] outputWaveform,
            double spectralDistance,
            double harmonicity,
            double clapProxy
    ) {}

    private LatentDiffusionInference() {}

    public static InferenceResult generate(
            double[] fanNoise,
            String prompt,
            LatentDiffusionConfig cfg,
            ScoreNetwork net
    ) {
        StftEngine.StftResult fanStft = StftEngine.forward(fanNoise, cfg.sampleRateHz);
        StftEncoder encoder = new StftEncoder(cfg.latentDim);
        TextConditioner conditioner = new TextConditioner(cfg.latentDim);
        DiffusionScheduler scheduler = new DiffusionScheduler(cfg.diffusionSteps, cfg.betaStart, cfg.betaEnd);
        Random rng = new Random(7);

        double[] fanLatent = encoder.encode(fanStft);
        double[] x = new double[cfg.latentDim];
        for (int i = 0; i < x.length; i++) {
            x[i] = 0.5 * fanLatent[i] + 0.5 * rng.nextGaussian();
        }
        double[] cond = conditioner.embed(prompt);

        for (int t = cfg.diffusionSteps - 1; t >= 0; t--) {
            double[] epsPred = net.predict(x, cond, t, cfg.diffusionSteps);
            x = scheduler.denoiseStep(x, epsPred, t);
        }

        StftEngine.StftResult outStft = encoder.decode(x, fanStft);
        double[] output = StftEngine.inverse(outStft, fanNoise.length);
        normalize(output, 0.9);

        double specDist = spectralDistance(output, cfg.sampleRateHz);
        double harm = harmonicity(output, cfg.sampleRateHz);
        double clap = clapProxy(output, prompt, cfg.sampleRateHz);

        return new InferenceResult(output, specDist, harm, clap);
    }

    public static ScoreNetwork loadNetwork(LatentDiffusionConfig cfg) throws IOException {
        return WeightSerializer.load(Path.of(cfg.weightsPath), cfg);
    }

    private static double spectralDistance(double[] x, int sr) {
        double mid = bandEnergy(x, sr, 400, 2000);
        double high = bandEnergy(x, sr, 2000, 6000);
        return Math.abs(10.0 * Math.log10(Math.max(1e-30, mid / Math.max(1e-30, high))));
    }

    private static double bandEnergy(double[] x, int sr, double fLo, double fHi) {
        double sum = 0;
        for (int i = 1; i < x.length / 2; i++) {
            double f = i * sr / (double) x.length;
            if (f >= fLo && f <= fHi) sum += x[i] * x[i];
        }
        return sum;
    }

    private static double harmonicity(double[] x, int sr) {
        int period = Math.max(1, (int) Math.round(sr / 220.0));
        if (period >= x.length) return 0;
        double corr = 0, energy = 0;
        for (int i = period; i < x.length; i++) {
            corr += x[i] * x[i - period];
            energy += x[i] * x[i];
        }
        return Math.max(0, corr / Math.max(1e-30, energy));
    }

    public static double clapProxy(double[] x, String prompt, int sr) {
        TextConditioner tc = new TextConditioner(32);
        double[] audioEmb = tc.embed("audio_" + spectralFingerprint(x, sr));
        double[] textEmb = tc.embed(prompt);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < audioEmb.length; i++) {
            dot += audioEmb[i] * textEmb[i];
            na += audioEmb[i] * audioEmb[i];
            nb += textEmb[i] * textEmb[i];
        }
        return dot / Math.sqrt(Math.max(1e-12, na) * Math.max(1e-12, nb));
    }

    private static String spectralFingerprint(double[] x, int sr) {
        double low = bandEnergy(x, sr, 100, 500);
        double mid = bandEnergy(x, sr, 500, 2000);
        double high = bandEnergy(x, sr, 2000, 8000);
        return String.format("l%.2f_m%.2f_h%.2f", low, mid, high);
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
