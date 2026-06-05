package com.heater.thermal;

import com.heater.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorIntegrationTest {

    @Test
    void winterDayFastRunPrimarySafe() throws Exception {
        var config = ConfigLoader.load(Path.of("config/default.yaml").toString());
        Simulator sim = new Simulator(config);
        ScenarioProfile scenario = ScenarioUtil.fromConfig(config, "winter_day");
        scenario = new ScenarioProfile(scenario.name(), 3600, scenario.qWasteBase(), scenario.qWastePeak(), scenario.outsideTemps());

        SimulationMetrics m = sim.run(scenario, null);
        assertEquals(0, m.primaryUnsafeTimeS(), 0.1);
    }

    @Test
    void climateFocusRemovesCo2() throws Exception {
        var config = ConfigLoader.load(Path.of("config/default.yaml").toString());
        Simulator sim = new Simulator(config);
        ScenarioProfile scenario = ScenarioUtil.fromConfig(config, "climate_focus");
        scenario = new ScenarioProfile(scenario.name(), 14400, scenario.qWasteBase(), scenario.qWastePeak(), scenario.outsideTemps());

        SimulationMetrics m = sim.run(scenario, null);
        assertEquals(0, m.primaryUnsafeTimeS(), 0.1);
        assertTrue(m.climate().grossRemovalKg() > 0, "expected CO2 removal in climate_focus scenario");
    }
}
