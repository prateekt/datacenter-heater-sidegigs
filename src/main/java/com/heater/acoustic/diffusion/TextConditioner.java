package com.heater.acoustic.diffusion;

import java.nio.charset.StandardCharsets;

/** Simple hashed prompt → fixed embedding (no T5). */
public final class TextConditioner {

    private final int embedDim;

    public TextConditioner(int embedDim) {
        this.embedDim = embedDim;
    }

    public double[] embed(String prompt) {
        double[] e = new double[embedDim];
        if (prompt == null || prompt.isBlank()) return e;
        byte[] bytes = prompt.toLowerCase().trim().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            int idx = (bytes[i] * 31 + i) % embedDim;
            e[idx] += 1.0;
        }
        double norm = 0;
        for (double v : e) norm += v * v;
        norm = Math.sqrt(Math.max(1e-12, norm));
        for (int i = 0; i < embedDim; i++) e[i] /= norm;
        return e;
    }
}
