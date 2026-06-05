package com.heater.acoustic.diffusion;

import com.heater.acoustic.StftEngine;

/** Compress log-magnitude STFT to fixed latent vector. */
public final class StftEncoder {

    private final int latentDim;

    public StftEncoder(int latentDim) {
        this.latentDim = latentDim;
    }

    public double[] encode(StftEngine.StftResult stft) {
        double[] flat = StftEngine.flattenLogMag(stft);
        return downsample(flat, latentDim);
    }

    public StftEngine.StftResult decode(double[] latent, StftEngine.StftResult template) {
        double[] flat = upsample(latent, template.frameCount() * template.binCount());
        return StftEngine.unflattenLogMag(flat, template);
    }

    static double[] downsample(double[] src, int dim) {
        double[] out = new double[dim];
        double block = (double) src.length / dim;
        for (int i = 0; i < dim; i++) {
            int lo = (int) (i * block);
            int hi = (int) Math.min(src.length, (i + 1) * block);
            double sum = 0;
            int count = 0;
            for (int j = lo; j < hi; j++) {
                sum += src[j];
                count++;
            }
            out[i] = count > 0 ? sum / count : 0;
        }
        return out;
    }

    static double[] upsample(double[] latent, int targetLen) {
        double[] out = new double[targetLen];
        double scale = (double) latent.length / targetLen;
        for (int i = 0; i < targetLen; i++) {
            int idx = Math.min(latent.length - 1, (int) (i * scale));
            out[i] = latent[idx];
        }
        return out;
    }
}
