package com.heater.thermal;

import com.heater.carbon.ConvectionCaptureConfig;
import com.heater.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionSimulatorIntegrationTest {

    @Test
    void convectionHybridRunCapturesCo2() throws Exception {
        var config = ConfigLoader.load(Path.of("config/nvidia_us_convection.yaml").toString());
        ConvectionCaptureConfig convection = ConvectionCaptureConfig.fromYaml(config);
        assertTrue(convection.convectionHybrid());

        Simulator sim = new Simulator(config);
        ScenarioProfile scenario = ScenarioUtil.fromConfig(config, "nvidia_us_module");
        scenario = new ScenarioProfile(
                scenario.name(), 7200, scenario.qWasteBase(), scenario.qWastePeak(), scenario.outsideTemps()
        );

        SimulationMetrics m = sim.run(scenario, null);
        assertEquals(0, m.primaryUnsafeTimeS(), 0.1);
        assertTrue(m.convectionAirflowM3s() > 0);
        assertTrue(m.fanPowerSavedMw() > 0);
        assertTrue(m.climate().grossRemovalKg() > 0);
    }
}
