package com.heater.thermal;

import com.heater.config.ConfigLoader;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ScenarioUtil {

    private ScenarioUtil() {}

    public static ScenarioProfile fromConfig(Map<String, Object> config, String name) {
        Map<String, Object> scenarios = ConfigLoader.map(config, "scenarios");
        Map<String, Object> sc = ConfigLoader.map(scenarios, name);
        List<Map<String, Object>> temps = (List<Map<String, Object>>) sc.get("outside_temps");
        return new ScenarioProfile(
                name,
                ConfigLoader.d(sc, "duration", 86400.0),
                ConfigLoader.d(sc, "q_waste_base", 200_000.0),
                ConfigLoader.d(sc, "q_waste_peak", 400_000.0),
                temps
        );
    }

    public static double interpolateOutsideTemp(double hour, List<Map<String, Object>> profile) {
        List<Map<String, Object>> points = profile.stream()
                .sorted(Comparator.comparingDouble(p -> ConfigLoader.d(p, "hour", 0.0)))
                .toList();
        if (points.isEmpty()) return 5.0;
        if (hour <= ConfigLoader.d(points.get(0), "hour", 0.0)) {
            return ConfigLoader.d(points.get(0), "temp", 5.0);
        }
        int last = points.size() - 1;
        if (hour >= ConfigLoader.d(points.get(last), "hour", 24.0)) {
            return ConfigLoader.d(points.get(last), "temp", 5.0);
        }
        for (int i = 0; i < points.size() - 1; i++) {
            double h0 = ConfigLoader.d(points.get(i), "hour", 0.0);
            double t0 = ConfigLoader.d(points.get(i), "temp", 0.0);
            double h1 = ConfigLoader.d(points.get(i + 1), "hour", 0.0);
            double t1 = ConfigLoader.d(points.get(i + 1), "temp", 0.0);
            if (h0 <= hour && hour <= h1) {
                double frac = h1 != h0 ? (hour - h0) / (h1 - h0) : 0.0;
                return t0 + frac * (t1 - t0);
            }
        }
        return ConfigLoader.d(points.get(points.size() - 1), "temp", 5.0);
    }

    public static double qWasteAtTime(double t, double base, double peak) {
        double hour = (t / 3600.0) % 24.0;
        if (hour >= 10 && hour <= 16) return peak;
        if (hour >= 18 && hour <= 22) return peak * 0.9;
        if (hour >= 6 && hour <= 9) return base * 1.2;
        return base;
    }
}
