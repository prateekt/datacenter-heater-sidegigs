package com.heater.acoustic;

import com.heater.config.ConfigLoader;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class FanOrchestraConfig {

    public boolean enabled = true;
    public double instrumentsPerFan = 1.0;
    public double instrumentedFraction = 1.0;
    public String instrumentType = "rotary_bow";
    public double fundamentalHz = 110.0;
    public int semitoneSpread = 24;
    public String bowCoverCycle = "tremobow,vase,curved";
    public double fanToBowGearRatio = 1.0;
    public double bpfEnergyReallocation = 0.12;
    public int statisticalVoiceSample = 128;

    public static FanOrchestraConfig fromMap(Map<String, Object> root) {
        Map<String, Object> m = ConfigLoader.map(root, "fan_orchestra");
        if (m.isEmpty()) {
            return new FanOrchestraConfig();
        }
        FanOrchestraConfig c = new FanOrchestraConfig();
        c.enabled = bool(m, "enabled", true);
        c.instrumentsPerFan = ConfigLoader.d(m, "instruments_per_fan", c.instrumentsPerFan);
        c.instrumentedFraction = ConfigLoader.d(m, "instrumented_fraction", c.instrumentedFraction);
        c.instrumentType = str(m, "instrument_type", c.instrumentType);
        c.fundamentalHz = ConfigLoader.d(m, "fundamental_hz", c.fundamentalHz);
        c.semitoneSpread = (int) ConfigLoader.d(m, "semitone_spread", c.semitoneSpread);
        c.bowCoverCycle = str(m, "bow_cover_cycle", c.bowCoverCycle);
        c.fanToBowGearRatio = ConfigLoader.d(m, "fan_to_bow_gear_ratio", c.fanToBowGearRatio);
        c.bpfEnergyReallocation = ConfigLoader.d(m, "bpf_energy_reallocation", c.bpfEnergyReallocation);
        c.statisticalVoiceSample = (int) ConfigLoader.d(m, "statistical_voice_sample", c.statisticalVoiceSample);
        return c;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "enabled", enabled,
                "instruments_per_fan", instrumentsPerFan,
                "instrumented_fraction", instrumentedFraction,
                "instrument_type", instrumentType,
                "fundamental_hz", fundamentalHz,
                "semitone_spread", semitoneSpread,
                "bow_cover_cycle", bowCoverCycle,
                "fan_to_bow_gear_ratio", fanToBowGearRatio,
                "bpf_energy_reallocation", bpfEnergyReallocation,
                "statistical_voice_sample", statisticalVoiceSample
        );
    }

    private static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : v.toString();
    }
}
