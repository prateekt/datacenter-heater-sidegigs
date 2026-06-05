package com.heater.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeatApplicationAnalyzerTest {

    @Test
    void analyzesThreeApplicationScenarios() throws Exception {
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load("config/gpu_profiles.yaml");
        List<HeatApplicationPoint> points = HeatApplicationAnalyzer.analyzeAll(
                "config/heat_applications.yaml",
                "config/nvidia_us_expansion.yaml",
                registry,
                3600.0
        );
        assertEquals(3, points.size());
        HeatApplicationPoint dac = points.stream().filter(p -> "dac_priority".equals(p.scenarioId())).findFirst().orElseThrow();
        HeatApplicationPoint community = points.stream().filter(p -> "community_heat".equals(p.scenarioId())).findFirst().orElseThrow();
        assertTrue(dac.netCo2eTonnesPerYear() > 0);
        assertTrue(dac.heatDacMwh() > community.heatDacMwh());
        assertTrue(community.heatPoolMwh() + community.heatAquacultureMwh() > 0);
    }
}
