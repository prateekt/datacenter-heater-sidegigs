package com.heater.acoustic.diffusion;

/** DDPM-style beta schedule for Java-LDM. */
public final class DiffusionScheduler {

    public final int steps;
    public final double[] betas;
    public final double[] alphas;
    public final double[] alphaBars;

    public DiffusionScheduler(int steps, double betaStart, double betaEnd) {
        this.steps = steps;
        this.betas = new double[steps];
        this.alphas = new double[steps];
        this.alphaBars = new double[steps];
        for (int t = 0; t < steps; t++) {
            betas[t] = betaStart + (betaEnd - betaStart) * t / Math.max(1, steps - 1);
            alphas[t] = 1.0 - betas[t];
            alphaBars[t] = t == 0 ? alphas[0] : alphaBars[t - 1] * alphas[t];
        }
    }

    public double[] addNoise(double[] x0, int t, java.util.Random rng) {
        double[] xt = new double[x0.length];
        double sqrtAb = Math.sqrt(alphaBars[t]);
        double sqrt1Ab = Math.sqrt(1.0 - alphaBars[t]);
        for (int i = 0; i < x0.length; i++) {
            xt[i] = sqrtAb * x0[i] + sqrt1Ab * rng.nextGaussian();
        }
        return xt;
    }

    public double[] denoiseStep(double[] xt, double[] epsPred, int t) {
        if (t <= 0) {
            double[] x0 = new double[xt.length];
            double invSqrtAb = 1.0 / Math.sqrt(Math.max(1e-12, alphaBars[t]));
            double coeff = (1 - alphas[t]) / Math.sqrt(Math.max(1e-12, 1 - alphaBars[t]));
            for (int i = 0; i < xt.length; i++) {
                x0[i] = invSqrtAb * (xt[i] - coeff * epsPred[i]);
            }
            return x0;
        }
        double[] xPrev = new double[xt.length];
        double alpha = alphas[t];
        double alphaBar = alphaBars[t];
        double beta = betas[t];
        double coef1 = 1.0 / Math.sqrt(alpha);
        double coef2 = beta / Math.sqrt(Math.max(1e-12, 1 - alphaBar));
        for (int i = 0; i < xt.length; i++) {
            xPrev[i] = coef1 * (xt[i] - coef2 * epsPred[i]);
        }
        return xPrev;
    }
}
