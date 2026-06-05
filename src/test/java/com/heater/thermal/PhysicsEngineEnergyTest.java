package com.heater.thermal;

import com.heater.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsEngineEnergyTest {

    @Test
    void recoveredPlusRejectedRespectsWasteHeatBudget() throws Exception {
        Map<String, Object> config = ConfigLoader.load("config/nvidia_us_expansion.yaml");
        Map<String, Object> scenarios = ConfigLoader.map(config, "scenarios");
        Map<String, Object> sc = ConfigLoader.map(scenarios, "nvidia_us_module");
        double base = ConfigLoader.d(sc, "q_waste_base", 0) * 2.0;
        double peak = ConfigLoader.d(sc, "q_waste_peak", 0) * 2.0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> temps = (List<Map<String, Object>>) sc.get("outside_temps");
        double durationS = 7200.0;
        ScenarioProfile scenario = new ScenarioProfile("nvidia_us_module", durationS, base, peak, temps);

        Simulator sim = new Simulator(config);
        sim.run(scenario, null);

        double avgWasteW = (base + peak) / 2.0;
        double recoveredW = sim.state.energyRecoveredJ / durationS;
        double rejectedW = sim.state.energyRejectedJ / durationS;

        assertTrue(rejectedW <= avgWasteW * 1.01,
                "rejected should not exceed average waste heat");
        assertTrue(recoveredW + rejectedW <= avgWasteW * 1.05,
                "first-law: delivered + unrecovered should not exceed exhaust");
    }
}
