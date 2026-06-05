package com.heater.acoustic.diffusion;

import java.util.Random;

/** Tiny MLP score network: predicts noise epsilon in latent space. */
public final class ScoreNetwork {

    public final int latentDim;
    public final int hiddenDim;
    public final int condDim;
    public final int inputDim;

    private final double[][] w1;
    private final double[] b1;
    private final double[][] w2;
    private final double[] b2;

    public ScoreNetwork(int latentDim, int hiddenDim, int condDim) {
        this.latentDim = latentDim;
        this.hiddenDim = hiddenDim;
        this.condDim = condDim;
        this.inputDim = latentDim + condDim + 1;
        this.w1 = new double[inputDim][hiddenDim];
        this.b1 = new double[hiddenDim];
        this.w2 = new double[hiddenDim][latentDim];
        this.b2 = new double[latentDim];
        initRandom(new Random(123));
    }

    public ScoreNetwork(int latentDim, int hiddenDim, int condDim, double[] flatWeights) {
        this.latentDim = latentDim;
        this.hiddenDim = hiddenDim;
        this.condDim = condDim;
        this.inputDim = latentDim + condDim + 1;
        this.w1 = new double[inputDim][hiddenDim];
        this.b1 = new double[hiddenDim];
        this.w2 = new double[hiddenDim][latentDim];
        this.b2 = new double[latentDim];
        loadFlat(flatWeights);
    }

    private void initRandom(Random rng) {
        for (int i = 0; i < inputDim; i++) {
            for (int j = 0; j < hiddenDim; j++) {
                w1[i][j] = rng.nextGaussian() * 0.05;
            }
        }
        for (int j = 0; j < hiddenDim; j++) {
            for (int k = 0; k < latentDim; k++) {
                w2[j][k] = rng.nextGaussian() * 0.05;
            }
        }
    }

    public double[] predict(double[] x, double[] cond, int step, int totalSteps) {
        double[] input = new double[inputDim];
        System.arraycopy(x, 0, input, 0, latentDim);
        System.arraycopy(cond, 0, input, latentDim, condDim);
        input[inputDim - 1] = step / (double) Math.max(1, totalSteps);

        double[] h = new double[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            double sum = b1[j];
            for (int i = 0; i < inputDim; i++) sum += input[i] * w1[i][j];
            h[j] = Math.max(0, sum);
        }
        double[] out = new double[latentDim];
        for (int k = 0; k < latentDim; k++) {
            double sum = b2[k];
            for (int j = 0; j < hiddenDim; j++) sum += h[j] * w2[j][k];
            out[k] = sum;
        }
        return out;
    }

    /** One SGD step on MSE(eps_pred, eps_true). Returns loss. */
    public double trainStep(double[] x, double[] cond, double[] epsTrue, int step, int totalSteps, double lr) {
        double[] input = new double[inputDim];
        System.arraycopy(x, 0, input, 0, latentDim);
        System.arraycopy(cond, 0, input, latentDim, condDim);
        input[inputDim - 1] = step / (double) Math.max(1, totalSteps);

        double[] hPre = new double[hiddenDim];
        double[] h = new double[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            double sum = b1[j];
            for (int i = 0; i < inputDim; i++) sum += input[i] * w1[i][j];
            hPre[j] = sum;
            h[j] = Math.max(0, sum);
        }
        double[] out = new double[latentDim];
        for (int k = 0; k < latentDim; k++) {
            double sum = b2[k];
            for (int j = 0; j < hiddenDim; j++) sum += h[j] * w2[j][k];
            out[k] = sum;
        }

        double loss = 0;
        double[] dOut = new double[latentDim];
        for (int k = 0; k < latentDim; k++) {
            double err = out[k] - epsTrue[k];
            loss += err * err;
            dOut[k] = 2 * err / latentDim;
        }

        double[][] dw2 = new double[hiddenDim][latentDim];
        double[] db2 = new double[latentDim];
        double[] dh = new double[hiddenDim];
        for (int k = 0; k < latentDim; k++) {
            db2[k] = dOut[k];
            for (int j = 0; j < hiddenDim; j++) {
                dw2[j][k] = h[j] * dOut[k];
                dh[j] += w2[j][k] * dOut[k];
            }
        }
        for (int j = 0; j < hiddenDim; j++) {
            if (hPre[j] <= 0) dh[j] = 0;
        }

        double[][] dw1 = new double[inputDim][hiddenDim];
        double[] db1 = new double[hiddenDim];
        for (int j = 0; j < hiddenDim; j++) {
            db1[j] = dh[j];
            for (int i = 0; i < inputDim; i++) {
                dw1[i][j] = input[i] * dh[j];
            }
        }

        for (int j = 0; j < hiddenDim; j++) {
            b1[j] -= lr * db1[j];
            for (int i = 0; i < inputDim; i++) {
                w1[i][j] -= lr * dw1[i][j];
            }
        }
        for (int j = 0; j < hiddenDim; j++) {
            for (int k = 0; k < latentDim; k++) {
                w2[j][k] -= lr * dw2[j][k];
            }
        }
        for (int k = 0; k < latentDim; k++) {
            b2[k] -= lr * db2[k];
        }

        return loss;
    }

    public double[] flatten() {
        int size = inputDim * hiddenDim + hiddenDim + hiddenDim * latentDim + latentDim;
        double[] flat = new double[size];
        int p = 0;
        for (int i = 0; i < inputDim; i++) {
            System.arraycopy(w1[i], 0, flat, p, hiddenDim);
            p += hiddenDim;
        }
        System.arraycopy(b1, 0, flat, p, hiddenDim);
        p += hiddenDim;
        for (int j = 0; j < hiddenDim; j++) {
            System.arraycopy(w2[j], 0, flat, p, latentDim);
            p += latentDim;
        }
        System.arraycopy(b2, 0, flat, p, latentDim);
        return flat;
    }

    private void loadFlat(double[] flat) {
        int p = 0;
        for (int i = 0; i < inputDim; i++) {
            System.arraycopy(flat, p, w1[i], 0, hiddenDim);
            p += hiddenDim;
        }
        System.arraycopy(flat, p, b1, 0, hiddenDim);
        p += hiddenDim;
        for (int j = 0; j < hiddenDim; j++) {
            System.arraycopy(flat, p, w2[j], 0, latentDim);
            p += latentDim;
        }
        System.arraycopy(flat, p, b2, 0, latentDim);
    }
}
