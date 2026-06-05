package com.heater;

import com.heater.carbon.ClimateImpactCalculator;
import com.heater.config.ConfigLoader;
import com.heater.robot.RobotTaskLog;
import com.heater.thermal.ScenarioProfile;
import com.heater.thermal.ScenarioUtil;
import com.heater.thermal.SimulationMetrics;
import com.heater.thermal.Simulator;

import java.util.Map;

public final class App {

    private App() {}

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    static int run(String[] args) throws Exception {
        String scenarioName = "winter_day";
        String configPath = null;
        Double duration = null;
        Double faultAt = null;
        boolean fast = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--scenario" -> scenarioName = args[++i];
                case "--config" -> configPath = args[++i];
                case "--duration" -> duration = Double.parseDouble(args[++i]);
                case "--fault-at" -> faultAt = Double.parseDouble(args[++i]);
                case "--fast" -> fast = true;
                default -> { }
            }
        }

        Map<String, Object> config = ConfigLoader.load(configPath);
        ScenarioProfile scenario = ScenarioUtil.fromConfig(config, scenarioName);
        if (duration != null) {
            scenario = new ScenarioProfile(
                    scenario.name(), duration, scenario.qWasteBase(),
                    scenario.qWastePeak(), scenario.outsideTemps()
            );
        } else if (fast) {
            scenario = new ScenarioProfile(
                    scenario.name(), 3600.0, scenario.qWasteBase(),
                    scenario.qWastePeak(), scenario.outsideTemps()
            );
        }

        Simulator sim = new Simulator(config);
        SimulationMetrics metrics = sim.run(scenario, faultAt);

        printReport(scenario, metrics);
        return metrics.primaryUnsafeTimeS() > 0 ? 1 : 0;
    }

    private static void printReport(ScenarioProfile scenario, SimulationMetrics metrics) {
        ClimateImpactCalculator.ClimateReport c = metrics.climate();

        System.out.println("Scenario: " + scenario.name());
        System.out.printf("Duration: %.0f s (%.1f h)%n", scenario.duration(), scenario.duration() / 3600.0);
        System.out.println();
        System.out.println("--- Energy ---");
        System.out.printf("Energy recovered:     %,.1f kWh%n", metrics.energyRecoveredKwh());
        System.out.printf("Energy rejected:      %,.1f kWh%n", metrics.energyRejectedKwh());
        System.out.println();
        System.out.println("--- CO2 Removal ---");
        System.out.printf("DAC CO2 captured:     %,.1f kg%n", c.dacCo2Kg());
        System.out.printf("Algae CO2 fixed:      %,.1f kg%n", c.algaeCo2Kg());
        System.out.printf("Gross removal:        %,.1f kg%n", c.grossRemovalKg());
        System.out.printf("Operational penalty:  %,.1f kg CO2e%n", c.operationalPenaltyKg());
        System.out.printf("Net CO2e removed:     %,.1f kg%n", c.netCo2eRemovedKg());
        System.out.println();
        System.out.println("--- Climate Impact (illustrative) ---");
        System.out.printf("Annualized net removal: %,.1f tonnes CO2e/yr%n", c.annualizedTonnesCo2e());
        System.out.printf("Radiative forcing offset: %.2f µW/m²%n", c.radiativeForcingOffsetWM2() * 1e6);
        System.out.printf("Warming offset:         %.2f mK (toy model)%n", c.warmingOffsetMilliKelvin());
        System.out.println();
        System.out.println("--- System ---");
        System.out.printf("Heat pump electricity:  %,.1f kWh%n", c.heatPumpElectricKwh());
        System.out.printf("CCS active time:        %.1f%%%n", c.ccsActivePct());
        System.out.printf("Primary unsafe time:    %.1f s%n", metrics.primaryUnsafeTimeS());
        System.out.printf("Pool satisfaction:      %.1f%%%n", metrics.poolSatisfactionPct());
        System.out.printf("House satisfaction:     %.1f%%%n", metrics.houseSatisfactionPct());
        System.out.println();
        System.out.println("--- Robot task log (last 15) ---");
        var events = metrics.robotEvents();
        int start = Math.max(0, events.size() - 15);
        for (int i = start; i < events.size(); i++) {
            RobotTaskLog e = events.get(i);
            String detail = e.detail().isEmpty() ? "" : " (" + e.detail() + ")";
            System.out.printf("  t=%8.0fs  %-18s  %s%s%n",
                    e.time(), e.event(), e.target().name().toLowerCase(), detail);
        }
    }
}
