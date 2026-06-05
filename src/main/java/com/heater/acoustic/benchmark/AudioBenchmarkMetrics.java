package com.heater.acoustic.benchmark;

import com.heater.acoustic.MechanicalDiffusionPhysics;
import com.heater.acoustic.MechanicalDiffusionPhysicsV2;
import com.heater.acoustic.MechanicalDiffusionConfig;
import com.heater.acoustic.MdmgLandscapeConfig;
import com.heater.acoustic.diffusion.LatentDiffusionInference;

public final class AudioBenchmarkMetrics {

    public record TierMetrics(
            String tier,
            double spectralDistance,
            double harmonicity,
            double roughness,
            double clapProxy,
            double fadProxy
    ) {}

    private AudioBenchmarkMetrics() {}

    public static TierMetrics fromV1(MechanicalDiffusionPhysics.DiffusionResult r, double clap) {
        return new TierMetrics("MDMG_v1", r.spectralDistanceToTemplate(), r.harmonicity(),
                r.roughnessProxy(), clap, fadProxy(r.outputWaveform(), 44100));
    }

    public static TierMetrics fromV2(MechanicalDiffusionPhysicsV2.V2Result r, double clap) {
        return fromV2Named(r, clap, "MDMG_v2");
    }

    public static TierMetrics fromV2Named(MechanicalDiffusionPhysicsV2.V2Result r, double clap, String name) {
        return new TierMetrics(name, r.spectralDistance(), r.harmonicity(),
                r.roughness(), clap, fadProxy(r.outputWaveform(), 44100));
    }

    public static TierMetrics fromLdm(LatentDiffusionInference.InferenceResult r) {
        return new TierMetrics("Java_LDM", r.spectralDistance(), r.harmonicity(),
                roughness(r.outputWaveform()), r.clapProxy(), fadProxy(r.outputWaveform(), 44100));
    }

    public static double clapProxyForWaveform(double[] x, String prompt, int sr) {
        return LatentDiffusionInference.clapProxy(x, prompt, sr);
    }

    static double roughness(double[] x) {
        double d = 0;
        for (int i = 1; i < x.length; i++) d += Math.abs(x[i] - x[i - 1]);
        return d / x.length;
    }

    /** Fréchet proxy: distance between log-mel mean/cov diagonal stats. */
    static double fadProxy(double[] x, int sr) {
        double[] mel = logMelMeans(x, sr);
        double target = 0.5;
        double dist = 0;
        for (double v : mel) {
            double d = v - target;
            dist += d * d;
        }
        return Math.sqrt(dist / mel.length);
    }

    private static double[] logMelMeans(double[] x, int sr) {
        int bands = 32;
        double[] mel = new double[bands];
        int[] count = new int[bands];
        for (int i = 1; i < x.length / 2; i++) {
            double f = i * sr / (double) x.length;
            int band = (int) (Math.log10(Math.max(20, f)) / Math.log10(8000) * (bands - 1));
            band = Math.max(0, Math.min(bands - 1, band));
            mel[band] += Math.log10(Math.max(1e-10, Math.abs(x[i])));
            count[band]++;
        }
        for (int b = 0; b < bands; b++) {
            if (count[b] > 0) mel[b] /= count[b];
        }
        return mel;
    }
}
