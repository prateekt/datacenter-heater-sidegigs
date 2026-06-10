package com.heater.acoustic.diffusion;

import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.acoustic.BowedStringSynthesizer;
import com.heater.acoustic.FanNoiseSpectrum;
import com.heater.acoustic.FanOrchestraConfig;
import com.heater.acoustic.MechanicalEqualizerConfig;

import java.util.Random;

/** Fan noise + fan-orchestra bowed-string targets for Java-LDM training. */
public final class ProceduralMusicCorpus {

    public record Pair(double[] fan, double[] music, String prompt) {}

    private ProceduralMusicCorpus() {}

    public static Pair[] generate(LatentDiffusionConfig cfg, AcousticSpectrumConfig specCfg) {
        return generate(cfg, specCfg, new FanOrchestraConfig());
    }

    public static Pair[] generate(
            LatentDiffusionConfig cfg,
            AcousticSpectrumConfig specCfg,
            FanOrchestraConfig orchestraCfg
    ) {
        Pair[] pairs = new Pair[cfg.corpusSize];
        Random rng = new Random(42);
        specCfg.clipDurationS = cfg.clipDurationS;
        specCfg.sampleRateHz = cfg.sampleRateHz;

        for (int i = 0; i < cfg.corpusSize; i++) {
            double[] fan = FanNoiseSpectrum.synthesizeWaveform(specCfg, rng);
            double[] music = BowedStringSynthesizer.synthesizeField(specCfg, orchestraCfg, rng);
            pairs[i] = new Pair(fan, music, cfg.defaultPrompt);
        }
        return pairs;
    }

    /** @deprecated use fan-orchestra synthesis via {@link #generate(LatentDiffusionConfig, AcousticSpectrumConfig)} */
    static double[] synthesizeAmbient(int sr, double durationS, Random rng, int seed) {
        MechanicalEqualizerConfig eq = MechanicalEqualizerConfig.fromMap(
                java.util.Map.of("mechanical_equalizer", java.util.Map.of(
                        "fan_orchestra", new FanOrchestraConfig().toMap())));
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.sampleRateHz = sr;
        spec.clipDurationS = durationS;
        return BowedStringSynthesizer.synthesizeField(spec, eq.fanOrchestra, rng);
    }
}
