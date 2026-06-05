package com.heater.acoustic.diffusion;

import com.heater.acoustic.AcousticSpectrumConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LatentDiffusionTrainerTest {

    @Test
    void trainAndSaveWeights(@TempDir Path temp) throws Exception {
        LatentDiffusionConfig cfg = new LatentDiffusionConfig();
        cfg.corpusSize = 4;
        cfg.trainingEpochs = 5;
        cfg.clipDurationS = 0.5;
        cfg.sampleRateHz = 8000;
        cfg.weightsPath = temp.resolve("score_network.bin").toString();
        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.clipDurationS = 0.5;
        spec.sampleRateHz = 8000;

        ScoreNetwork net = LatentDiffusionTrainer.train(cfg, spec);
        assertNotNull(net);
        assertTrue(java.nio.file.Files.exists(Path.of(cfg.weightsPath)));
    }
}
