package com.heater.acoustic;

import java.util.Random;

/**
 * MDMG v2 — STFT-latent overdamped Langevin dynamics with trainable diagonal landscape K.
 * No post-hoc harmonic injection.
 */
public final class MechanicalDiffusionPhysicsV2 {

    public record V2Result(
            double[] outputWaveform,
            double spectralDistance,
            double harmonicity,
            double roughness,
            double energyDissipated,
            int reverseSteps
    ) {}

    private MechanicalDiffusionPhysicsV2() {}

    public static V2Result denoise(
            double[] fanNoise,
            MdmgLandscapeConfig landscape,
            Random rng
    ) {
        StftEngine.StftResult stft = StftEngine.forward(fanNoise, 44100);
        double[] x = StftEngine.flattenLogMag(stft);
        double energy = 0.0;

        for (int step = 0; step < landscape.reverseSteps; step++) {
            double t = step / (double) Math.max(1, landscape.reverseSteps - 1);
            double sigma = landscape.noiseScheduleStart
                    + t * (landscape.noiseScheduleEnd - landscape.noiseScheduleStart);

            double[] grad = gradient(x, landscape);
            for (int i = 0; i < x.length; i++) {
                double noise = rng.nextGaussian() * sigma * 0.005;
                double dx = -landscape.damping * grad[i] * landscape.dt + noise;
                x[i] += dx;
                energy += Math.abs(dx * grad[i]);
            }
        }

        StftEngine.StftResult outStft = StftEngine.unflattenLogMag(x, stft);
        double[] output = StftEngine.inverse(outStft, fanNoise.length);
        normalize(output, 0.9);

        return new V2Result(
                output,
                spectralDistance(output, 44100),
                harmonicity(output, 44100),
                roughness(output),
                energy,
                landscape.reverseSteps
        );
    }

    private static double[] gradient(double[] x, MdmgLandscapeConfig cfg) {
        double[] g = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            g[i] = cfg.stiffnessAt(i, x.length) * x[i];
        }
        return g;
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

    private static double roughness(double[] x) {
        double d = 0;
        for (int i = 1; i < x.length; i++) d += Math.abs(x[i] - x[i - 1]);
        return d / x.length;
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
