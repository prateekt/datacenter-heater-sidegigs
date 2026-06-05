package com.heater.acoustic.diffusion;

import com.heater.acoustic.MdmgLandscapeConfig;
import com.heater.acoustic.StftEngine;
import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Distill Java-LDM score trajectories into mechanical diagonal K for MDMG v2. */
public final class MdmgLandscapeTrainer {

    public record DistillResult(double[] diagonalK, double mse, int latentLength) {}

    private MdmgLandscapeTrainer() {}

    public static DistillResult distill(
            LatentDiffusionConfig ldmCfg,
            AcousticSpectrumConfig specCfg,
            ScoreNetwork net
    ) throws IOException {
        ProceduralMusicCorpus.Pair[] corpus = ProceduralMusicCorpus.generate(ldmCfg, specCfg);
        StftEncoder encoder = new StftEncoder(ldmCfg.latentDim);
        TextConditioner conditioner = new TextConditioner(ldmCfg.latentDim);
        DiffusionScheduler scheduler = new DiffusionScheduler(ldmCfg.diffusionSteps, ldmCfg.betaStart, ldmCfg.betaEnd);
        Random rng = new Random(55);

        int stftLen = 0;
        List<double[]> scoreDirs = new ArrayList<>();
        List<double[]> states = new ArrayList<>();

        for (ProceduralMusicCorpus.Pair pair : corpus) {
            StftEngine.StftResult fanStft = StftEngine.forward(pair.fan(), ldmCfg.sampleRateHz);
            double[] fanFlat = StftEngine.flattenLogMag(fanStft);
            stftLen = fanFlat.length;

            double[] fanLatent = encoder.encode(fanStft);
            double[] x = fanLatent.clone();
            double[] cond = conditioner.embed(pair.prompt());

            for (int t = ldmCfg.diffusionSteps - 1; t >= 0; t--) {
                double[] xBefore = x.clone();
                double[] epsPred = net.predict(x, cond, t, ldmCfg.diffusionSteps);
                x = scheduler.denoiseStep(x, epsPred, t);
                double[] delta = new double[x.length];
                for (int i = 0; i < delta.length; i++) delta[i] = x[i] - xBefore[i];
                scoreDirs.add(delta);
                states.add(xBefore);
            }

            double[] stftX = StftEngine.flattenLogMag(fanStft);
            for (int t = ldmCfg.diffusionSteps - 1; t >= 0; t--) {
                double[] epsPred = net.predict(encoder.encode(fanStft), cond, t, ldmCfg.diffusionSteps);
                double[] mapped = StftEncoder.upsample(epsPred, stftLen);
                scoreDirs.add(mapped);
                states.add(stftX);
            }
        }

        double[] diagK = new double[stftLen];
        double[] sumK = new double[stftLen];
        int[] count = new int[stftLen];

        for (int s = 0; s < scoreDirs.size(); s++) {
            double[] delta = scoreDirs.get(s);
            double[] state = states.get(s);
            int len = Math.min(delta.length, stftLen);
            for (int i = 0; i < len; i++) {
                if (Math.abs(state[i % state.length]) > 1e-6) {
                    double k = Math.abs(delta[i % delta.length] / state[i % state.length]);
                    sumK[i] += Math.min(1.0, k);
                    count[i]++;
                }
            }
        }

        for (int i = 0; i < stftLen; i++) {
            diagK[i] = count[i] > 0 ? sumK[i] / count[i] : 0.05;
            if (diagK[i] < 0.01) diagK[i] = 0.05;
        }

        double totalErr = 0;
        int errCount = 0;
        for (int s = 0; s < scoreDirs.size(); s++) {
            double[] delta = scoreDirs.get(s);
            double[] state = states.get(s);
            int len = Math.min(delta.length, stftLen);
            for (int i = 0; i < len; i++) {
                double pred = diagK[i] * state[i % state.length];
                totalErr += (pred - delta[i % delta.length]) * (pred - delta[i % delta.length]);
                errCount++;
            }
        }
        double mse = errCount > 0 ? totalErr / errCount : 1.0;
        return new DistillResult(diagK, mse, stftLen);
    }

    public static void exportYaml(Path path, DistillResult result, MdmgLandscapeConfig template) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Mechanical diffusion landscape (distilled from Java-LDM)\n");
        sb.append("mdmg_landscape:\n");
        sb.append("  enabled: true\n");
        sb.append("  reverse_steps: ").append(template.reverseSteps).append("\n");
        sb.append("  dt: ").append(template.dt).append("\n");
        sb.append("  damping: ").append(template.damping).append("\n");
        sb.append("  noise_schedule_start: ").append(template.noiseScheduleStart).append("\n");
        sb.append("  noise_schedule_end: ").append(template.noiseScheduleEnd).append("\n");
        sb.append("  distillation_mse: ").append(String.format("%.6f", result.mse())).append("\n");
        sb.append("  diagonal_k:\n");
        int step = Math.max(1, result.diagonalK().length / 64);
        for (int i = 0; i < result.diagonalK().length; i += step) {
            sb.append("    - ").append(String.format("%.6f", result.diagonalK()[i])).append("\n");
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, sb.toString());
        System.out.println("Wrote landscape to " + path);
    }

    public static MdmgLandscapeConfig loadExpandedLandscape(Path path, int targetLen) throws IOException {
        Map<String, Object> root = ConfigLoader.load(path.toString());
        MdmgLandscapeConfig cfg = MdmgLandscapeConfig.fromMap(root);
        if (cfg.diagonalK.length > 0 && cfg.diagonalK.length < targetLen) {
            double[] expanded = new double[targetLen];
            for (int i = 0; i < targetLen; i++) {
                int idx = i * cfg.diagonalK.length / targetLen;
                expanded[i] = cfg.diagonalK[Math.min(idx, cfg.diagonalK.length - 1)];
            }
            cfg.diagonalK = expanded;
        } else if (cfg.diagonalK.length == 0) {
            cfg.diagonalK = new double[targetLen];
            for (int i = 0; i < targetLen; i++) cfg.diagonalK[i] = 0.05;
        }
        return cfg;
    }
}
