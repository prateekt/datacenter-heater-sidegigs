package com.heater.acoustic.diffusion;

import com.heater.acoustic.AcousticSpectrumConfig;
import com.heater.acoustic.StftEngine;
import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

/** Pure Java training for task-specific latent diffusion score network. */
public final class LatentDiffusionTrainer {

    private LatentDiffusionTrainer() {}

    public static ScoreNetwork train(String configPath, AcousticSpectrumConfig specCfg) throws IOException {
        Map<String, Object> root = ConfigLoader.load(configPath);
        LatentDiffusionConfig cfg = LatentDiffusionConfig.fromMap(root);
        return train(cfg, specCfg);
    }

    public static ScoreNetwork train(LatentDiffusionConfig cfg, AcousticSpectrumConfig specCfg) throws IOException {
        ProceduralMusicCorpus.Pair[] corpus = ProceduralMusicCorpus.generate(cfg, specCfg);
        StftEncoder encoder = new StftEncoder(cfg.latentDim);
        TextConditioner conditioner = new TextConditioner(cfg.latentDim);
        DiffusionScheduler scheduler = new DiffusionScheduler(cfg.diffusionSteps, cfg.betaStart, cfg.betaEnd);
        ScoreNetwork net = new ScoreNetwork(cfg.latentDim, cfg.hiddenDim, cfg.latentDim);
        Random rng = new Random(99);

        for (int epoch = 0; epoch < cfg.trainingEpochs; epoch++) {
            double epochLoss = 0;
            int batches = 0;
            for (ProceduralMusicCorpus.Pair pair : corpus) {
                StftEngine.StftResult musicStft = StftEngine.forward(pair.music(), cfg.sampleRateHz);
                double[] x0 = encoder.encode(musicStft);
                double[] cond = conditioner.embed(pair.prompt());
                int t = rng.nextInt(cfg.diffusionSteps);
                double[] eps = new double[x0.length];
                for (int i = 0; i < eps.length; i++) eps[i] = rng.nextGaussian();
                double[] xt = scheduler.addNoise(x0, t, rng);
                double[] fanLatent = encoder.encode(StftEngine.forward(pair.fan(), cfg.sampleRateHz));
                for (int i = 0; i < xt.length; i++) {
                    xt[i] = 0.7 * xt[i] + 0.3 * fanLatent[i];
                }
                epochLoss += net.trainStep(xt, cond, eps, t, cfg.diffusionSteps, cfg.learningRate);
                batches++;
            }
            if (epoch % 10 == 0) {
                System.out.printf("Epoch %d loss=%.6f%n", epoch, epochLoss / Math.max(1, batches));
            }
        }

        Path weights = Path.of(cfg.weightsPath);
        WeightSerializer.save(weights, net);
        System.out.println("Saved weights to " + weights);
        return net;
    }
}
