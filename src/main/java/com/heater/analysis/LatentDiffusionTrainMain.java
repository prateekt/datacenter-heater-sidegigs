package com.heater.analysis;

import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.acoustic.diffusion.LatentDiffusionConfig;
import com.heater.acoustic.diffusion.LatentDiffusionTrainer;
import com.heater.config.ConfigLoader;

public final class LatentDiffusionTrainMain {

    private LatentDiffusionTrainMain() {}

    public static void main(String[] args) throws Exception {
        String ldmPath = "config/latent_diffusion.yaml";
        String spectrumPath = "config/acoustic_spectrum.yaml";
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i])) ldmPath = args[++i];
            if ("--spectrum".equals(args[i])) spectrumPath = args[++i];
        }
        AcousticSpectrumConfig spec = AcousticSpectrumConfig.fromMap(ConfigLoader.load(spectrumPath));
        LatentDiffusionTrainer.train(ldmPath, spec);
    }
}
