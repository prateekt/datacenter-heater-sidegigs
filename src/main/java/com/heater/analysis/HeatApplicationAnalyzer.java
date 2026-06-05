package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.SimulationMetrics;
import com.heater.thermal.Simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class HeatApplicationAnalyzer {

    private HeatApplicationAnalyzer() {}

    public static List<HeatApplicationPoint> analyzeAll(
            String applicationsPath,
            String referenceConfigPath,
            GpuProfile.GpuProfileRegistry registry,
            double simDurationS
    ) throws IOException {
        Map<String, Object> appsRoot = ConfigLoader.load(applicationsPath);
        Map<String, Object> refConfig = ConfigLoader.load(referenceConfigPath);
        GpuProfile refProfile = registry.referenceProfile();
        int refGpus = registry.referenceGpuCount();

        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) appsRoot.get("application_scenarios");
        List<HeatApplicationPoint> results = new ArrayList<>();
        if (scenarios == null) return results;

        double olympicKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "olympic_pool"), "annual_heat_kwh", 180_000);
        double communityPoolKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "community_pool"), "annual_heat_kwh", 45_000);
        double racewayM3 = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "raceway_volume_m3", 500);
        double wPerM3 = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "maintenance_w_per_m3", 55);
        double fishKgM3Yr = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "fish_kg_per_m3_year", 25);
        double homeKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "district_heat"), "us_home_annual_heat_kwh", 8000);
        double racewayKwhPerYear = wPerM3 * racewayM3 * 8760.0 / 1000.0;

        for (Map<String, Object> sc : scenarios) {
            String id = sc.get("id").toString();
            String label = sc.getOrDefault("label", id).toString();
            String configPath = sc.get("config").toString();
            Map<String, Object> scenarioConfig = ConfigLoader.load(configPath);

            FacilityScaler.ScaledFacility facility = FacilityScaler.scale(
                    mergeRouting(scenarioConfig, refConfig),
                    "nvidia_us_module",
                    refProfile,
                    refGpus,
                    registry,
                    FacilityScaler.ScaleMode.PROPORTIONAL,
                    simDurationS
            );

            Simulator sim = new Simulator(facility.config());
            SimulationMetrics metrics = sim.run(facility.scenario(), null);
            double annualFactor = simDurationS > 0 ? (365.0 * 86400.0 / simDurationS) : 0.0;

            double poolMwh = sim.state.energyPoolJ / 3_600_000_000.0 * annualFactor;
            double aquaMwh = sim.state.energyAquacultureJ / 3_600_000_000.0 * annualFactor;
            double algaeMwh = sim.state.energyAlgaeJ / 3_600_000_000.0 * annualFactor;
            double dacMwh = sim.state.energyDacJ / 3_600_000_000.0 * annualFactor;
            double totalMwh = poolMwh + aquaMwh + algaeMwh + dacMwh;

            double netTonnes = metrics.climate().annualizedTonnesCo2e();
            double aquaRaceways = aquaMwh * 1000.0 / racewayKwhPerYear;
            double fishKg = aquaRaceways * racewayM3 * fishKgM3Yr;

            results.add(new HeatApplicationPoint(
                    id,
                    label,
                    netTonnes,
                    poolMwh,
                    aquaMwh,
                    algaeMwh,
                    dacMwh,
                    totalMwh,
                    poolMwh * 1000.0 / olympicKwh,
                    poolMwh * 1000.0 / communityPoolKwh,
                    aquaRaceways,
                    fishKg,
                    sim.state.algae.surfaceAreaM2 / 10_000.0,
                    totalMwh * 1000.0 / homeKwh,
                    metrics.poolSatisfactionPct()
            ));
        }
        return results;
    }

    /** Keep plant sizing from reference; copy robot priority and load defs from scenario. */
    private static Map<String, Object> mergeRouting(Map<String, Object> scenario, Map<String, Object> reference) {
        Map<String, Object> merged = ConfigDeepCopy.copy(reference);
        if (scenario.containsKey("robot")) {
            merged.put("robot", ConfigDeepCopy.copy(scenario).get("robot"));
        }
        if (scenario.containsKey("loads")) {
            merged.put("loads", ConfigDeepCopy.copy(scenario).get("loads"));
        }
        return merged;
    }

    public static String formatPoint(HeatApplicationPoint p) {
        return String.format(Locale.US,
                "**%s** — **%,.0f tonnes CO₂e/yr** net. Heat delivered: **%,.0f MWh/yr** total "
                        + "(pools **%,.0f** · fisheries **%,.0f** · algae **%,.0f** · DAC **%,.0f**). "
                        + "≈ **%.1f olympic pools**, **%.1f raceways** (500 m³), **~%,.0f kg fish/yr** potential, "
                        + "**%.1f ha** algae, **~%,.0f homes** heat equivalent.",
                p.label(), p.netCo2eTonnesPerYear(), p.heatTotalMwh(),
                p.heatPoolMwh(), p.heatAquacultureMwh(), p.heatAlgaeMwh(), p.heatDacMwh(),
                p.olympicPoolsEquivalent(), p.aquacultureRacewaysEquivalent(), p.fishProductionKgPerYear(),
                p.algaeHectaresEquivalent(), p.homesHeatedEquivalent());
    }
}
