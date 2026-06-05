package com.heater.acoustic;

import com.heater.config.ConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class MdmgLandscapeConfig {

    public boolean enabled = true;
    public int reverseSteps = 40;
    public double dt = 0.05;
    public double damping = 0.2;
    public double noiseScheduleStart = 0.4;
    public double noiseScheduleEnd = 0.02;
    public double[] diagonalK = new double[0];
    public double distillationMse = 0.0;

    public static MdmgLandscapeConfig fromMap(Map<String, Object> root) {
        Map<String, Object> m = ConfigLoader.map(root, "mdmg_landscape");
        MdmgLandscapeConfig c = new MdmgLandscapeConfig();
        c.enabled = m.get("enabled") instanceof Boolean b ? b : true;
        c.reverseSteps = (int) ConfigLoader.d(m, "reverse_steps", c.reverseSteps);
        c.dt = ConfigLoader.d(m, "dt", c.dt);
        c.damping = ConfigLoader.d(m, "damping", c.damping);
        c.noiseScheduleStart = ConfigLoader.d(m, "noise_schedule_start", c.noiseScheduleStart);
        c.noiseScheduleEnd = ConfigLoader.d(m, "noise_schedule_end", c.noiseScheduleEnd);
        c.distillationMse = ConfigLoader.d(m, "distillation_mse", c.distillationMse);
        c.diagonalK = doubleList(m, "diagonal_k");
        return c;
    }

    public static double[] doubleList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list && !list.isEmpty()) {
            return list.stream().mapToDouble(o -> ((Number) o).doubleValue()).toArray();
        }
        return new double[0];
    }

    public double stiffnessAt(int index, int length) {
        if (diagonalK.length == 0) return 0.05;
        if (index < diagonalK.length) return diagonalK[index];
        return diagonalK[diagonalK.length - 1];
    }
}
