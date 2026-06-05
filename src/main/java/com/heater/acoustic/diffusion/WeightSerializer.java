package com.heater.acoustic.diffusion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/** Binary weight serializer for ScoreNetwork. */
public final class WeightSerializer {

    private static final int MAGIC = 0x534E544D; // SNTM

    private WeightSerializer() {}

    public static void save(Path path, ScoreNetwork net) throws IOException {
        Files.createDirectories(path.getParent());
        double[] flat = net.flatten();
        ByteBuffer buf = ByteBuffer.allocate(16 + flat.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(MAGIC);
        buf.putInt(net.latentDim);
        buf.putInt(net.hiddenDim);
        buf.putInt(net.condDim);
        for (double v : flat) buf.putDouble(v);
        Files.write(path, buf.array());
    }

    public static ScoreNetwork load(Path path, LatentDiffusionConfig cfg) throws IOException {
        if (!Files.exists(path)) {
            return new ScoreNetwork(cfg.latentDim, cfg.hiddenDim, cfg.latentDim);
        }
        ByteBuffer buf = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buf.getInt();
        if (magic != MAGIC) {
            return new ScoreNetwork(cfg.latentDim, cfg.hiddenDim, cfg.latentDim);
        }
        int latent = buf.getInt();
        int hidden = buf.getInt();
        int cond = buf.getInt();
        double[] flat = new double[buf.remaining() / 8];
        for (int i = 0; i < flat.length; i++) flat[i] = buf.getDouble();
        return new ScoreNetwork(latent, hidden, cond, flat);
    }
}
