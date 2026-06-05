package com.heater.analysis;

import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.acoustic.MdmgLandscapeConfig;
import com.heater.acoustic.diffusion.*;
import com.heater.config.ConfigLoader;

import java.nio.file.Path;

public final class MdmgLandscapeTrainMain {

    private MdmgLandscapeTrainMain() {}

    public static void main(String[] args) throws Exception {
        String ldmPath = "config/latent_diffusion.yaml";
        String spectrumPath = "config/acoustic_spectrum.yaml";
        String landscapePath = "config/mdmg_landscape.yaml";
        for (int i = 0; i < args.length; i++) {
            if ("--ldm".equals(args[i])) ldmPath = args[++i];
            if ("--spectrum".equals(args[i])) spectrumPath = args[++i];
            if ("--landscape".equals(args[i])) landscapePath = args[++i];
        }
        LatentDiffusionConfig ldmCfg = LatentDiffusionConfig.fromMap(ConfigLoader.load(ldmPath));
        AcousticSpectrumConfig spec = AcousticSpectrumConfig.fromMap(ConfigLoader.load(spectrumPath));
        ScoreNetwork net = LatentDiffusionInference.loadNetwork(ldmCfg);
        var result = MdmgLandscapeTrainer.distill(ldmCfg, spec, net);
        MdmgLandscapeConfig template = MdmgLandscapeConfig.fromMap(ConfigLoader.load(landscapePath));
        MdmgLandscapeTrainer.exportYaml(Path.of(landscapePath), result, template);
    }
}
