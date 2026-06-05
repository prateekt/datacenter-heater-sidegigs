package com.heater.analysis;

import com.heater.acoustic.*;
import com.heater.acoustic.benchmark.AudioBenchmarkMetrics;
import com.heater.acoustic.diffusion.*;
import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unchecked")
public final class MdmgBenchmarkMain {

    private MdmgBenchmarkMain() {}

    public static void main(String[] args) throws Exception {
        String configPath = "config/mdmg_benchmark.yaml";
        Path readmePath = Path.of("README.md");
        boolean patchReadme = true;
        boolean skipTrain = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config" -> configPath = args[++i];
                case "--no-readme" -> patchReadme = false;
                case "--skip-train" -> skipTrain = true;
                default -> { }
            }
        }

        Map<String, Object> benchCfg = ConfigLoader.load(configPath);
        Map<String, Object> bench = ConfigLoader.map(benchCfg, "mdmg_benchmark");
        String spectrumPath = bench.get("spectrum_config").toString();
        String ldmPath = bench.get("latent_diffusion_config").toString();
        String landscapePath = bench.get("landscape_config").toString();
        String prompt = bench.get("prompt").toString();
        Path outputDir = Path.of(bench.get("output_dir").toString());
        Path resultsPath = Path.of(bench.get("results_json").toString());

        AcousticSpectrumConfig specCfg = AcousticSpectrumConfig.fromMap(ConfigLoader.load(spectrumPath));
        LatentDiffusionConfig ldmCfg = LatentDiffusionConfig.fromMap(ConfigLoader.load(ldmPath));
        MechanicalDiffusionConfig mdmgV1Cfg = MechanicalDiffusionConfig.fromMap(
                ConfigLoader.load("config/mechanical_diffusion.yaml"));

        if (!skipTrain && !Files.exists(Path.of(ldmCfg.weightsPath))) {
            System.out.println("Training Java-LDM...");
            LatentDiffusionTrainer.train(ldmCfg, specCfg);
        }

        ScoreNetwork net = LatentDiffusionInference.loadNetwork(ldmCfg);
        if (!skipTrain) {
            System.out.println("Distilling MDMG landscape...");
            var distill = MdmgLandscapeTrainer.distill(ldmCfg, specCfg, net);
            MdmgLandscapeConfig template = MdmgLandscapeConfig.fromMap(ConfigLoader.load(landscapePath));
            MdmgLandscapeTrainer.exportYaml(Path.of(landscapePath), distill, template);
        }

        Random rng = new Random(42);
        double[] fan = FanNoiseSpectrum.synthesizeWaveform(specCfg, rng);

        var v1 = MechanicalDiffusionPhysics.denoise(mdmgV1Cfg, fan, rng);
        WavExporter.writeMono(outputDir.resolve("mdmg_v1_baseline.wav"), v1.outputWaveform(), specCfg.sampleRateHz);

        StftEngine.StftResult fanStft = StftEngine.forward(fan, specCfg.sampleRateHz);
        MdmgLandscapeConfig landscapeDefault = MdmgLandscapeConfig.fromMap(ConfigLoader.load(landscapePath));
        MdmgLandscapeConfig landscapeExpanded = MdmgLandscapeTrainer.loadExpandedLandscape(
                Path.of(landscapePath), StftEngine.flattenLogMag(fanStft).length);

        var v2 = MechanicalDiffusionPhysicsV2.denoise(fan, landscapeDefault, rng);
        WavExporter.writeMono(outputDir.resolve("mdmg_v2.wav"), v2.outputWaveform(), specCfg.sampleRateHz);

        var v2dist = MechanicalDiffusionPhysicsV2.denoise(fan, landscapeExpanded, new Random(42));
        WavExporter.writeMono(outputDir.resolve("mdmg_v2_distilled.wav"), v2dist.outputWaveform(), specCfg.sampleRateHz);

        var ldm = LatentDiffusionInference.generate(fan, prompt, ldmCfg, net);
        WavExporter.writeMono(outputDir.resolve("java_ldm_output.wav"), ldm.outputWaveform(), specCfg.sampleRateHz);

        List<AudioBenchmarkMetrics.TierMetrics> tiers = List.of(
                AudioBenchmarkMetrics.fromV1(v1, AudioBenchmarkMetrics.clapProxyForWaveform(v1.outputWaveform(), prompt, specCfg.sampleRateHz)),
                AudioBenchmarkMetrics.fromV2(v2, AudioBenchmarkMetrics.clapProxyForWaveform(v2.outputWaveform(), prompt, specCfg.sampleRateHz)),
                AudioBenchmarkMetrics.fromV2Named(v2dist, AudioBenchmarkMetrics.clapProxyForWaveform(v2dist.outputWaveform(), prompt, specCfg.sampleRateHz), "MDMG_v2_distilled"),
                AudioBenchmarkMetrics.fromLdm(ldm)
        );

        double distillMse = landscapeExpanded.distillationMse;
        String json = toJson(tiers, distillMse);
        Files.createDirectories(resultsPath.getParent());
        Files.writeString(resultsPath, json);
        System.out.println("Wrote " + resultsPath);

        if (patchReadme && Files.exists(readmePath)) {
            String md = MdmgBenchmarkExplainer.explain(tiers, landscapeExpanded.distillationMse, prompt);
            ReadmePatcher.patchMdmgBenchmark(readmePath, md);
            System.out.println("Patched README MDMG benchmark section");
        }
    }

    private static String toJson(List<AudioBenchmarkMetrics.TierMetrics> tiers, double distillMse) {
        StringBuilder sb = new StringBuilder("{\n  \"distillation_mse\": ").append(String.format(Locale.US, "%.6f", distillMse)).append(",\n  \"tiers\": [\n");
        for (int i = 0; i < tiers.size(); i++) {
            AudioBenchmarkMetrics.TierMetrics t = tiers.get(i);
            if (i > 0) sb.append(",\n");
            sb.append("    {\"tier\":\"").append(t.tier()).append("\",");
            sb.append("\"spectral_distance\":").append(fmt(t.spectralDistance())).append(",");
            sb.append("\"harmonicity\":").append(fmt(t.harmonicity())).append(",");
            sb.append("\"clap_proxy\":").append(fmt(t.clapProxy())).append(",");
            sb.append("\"fad_proxy\":").append(fmt(t.fadProxy())).append("}");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.4f", v);
    }
}
