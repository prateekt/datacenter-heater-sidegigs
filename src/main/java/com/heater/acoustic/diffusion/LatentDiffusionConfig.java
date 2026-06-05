package com.heater.acoustic.diffusion;

import com.heater.config.ConfigLoader;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class LatentDiffusionConfig {

    public boolean enabled = true;
    public int latentDim = 64;
    public int hiddenDim = 128;
    public int diffusionSteps = 30;
    public int trainingEpochs = 40;
    public double learningRate = 0.002;
    public int batchSize = 8;
    public int corpusSize = 32;
    public int sampleRateHz = 44100;
    public double clipDurationS = 2.0;
    public String defaultPrompt = "gentle acoustic piano 60 BPM calm fence-line soundscape";
    public String weightsPath = "models/score_network.bin";
    public double betaStart = 0.0001;
    public double betaEnd = 0.02;

    public static LatentDiffusionConfig fromMap(Map<String, Object> root) {
        Map<String, Object> m = ConfigLoader.map(root, "latent_diffusion");
        LatentDiffusionConfig c = new LatentDiffusionConfig();
        c.enabled = m.get("enabled") instanceof Boolean b ? b : true;
        c.latentDim = (int) ConfigLoader.d(m, "latent_dim", c.latentDim);
        c.hiddenDim = (int) ConfigLoader.d(m, "hidden_dim", c.hiddenDim);
        c.diffusionSteps = (int) ConfigLoader.d(m, "diffusion_steps", c.diffusionSteps);
        c.trainingEpochs = (int) ConfigLoader.d(m, "training_epochs", c.trainingEpochs);
        c.learningRate = ConfigLoader.d(m, "learning_rate", c.learningRate);
        c.batchSize = (int) ConfigLoader.d(m, "batch_size", c.batchSize);
        c.corpusSize = (int) ConfigLoader.d(m, "corpus_size", c.corpusSize);
        c.sampleRateHz = (int) ConfigLoader.d(m, "sample_rate_hz", c.sampleRateHz);
        c.clipDurationS = ConfigLoader.d(m, "clip_duration_s", c.clipDurationS);
        c.defaultPrompt = String.valueOf(m.getOrDefault("default_prompt", c.defaultPrompt));
        c.weightsPath = String.valueOf(m.getOrDefault("weights_path", c.weightsPath));
        c.betaStart = ConfigLoader.d(m, "beta_start", c.betaStart);
        c.betaEnd = ConfigLoader.d(m, "beta_end", c.betaEnd);
        return c;
    }
}
