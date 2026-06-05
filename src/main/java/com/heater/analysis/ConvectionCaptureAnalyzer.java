package com.heater.analysis;

import com.heater.carbon.ConvectionCaptureConfig;
import com.heater.carbon.ConvectionCaptureCycle;
import com.heater.carbon.ConvectionCapturePhysics;
import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ConvectionCaptureAnalyzer {

    private final Map<String, Object> sweepConfig;
    private final Map<String, Object> captureDefaults;

    public ConvectionCaptureAnalyzer(String sweepPath, String defaultsPath) throws IOException {
        this.sweepConfig = ConfigLoader.load(sweepPath);
        this.captureDefaults = ConfigLoader.load(defaultsPath);
    }

    public ConvectionResultsSummary runAll() throws IOException {
        ConvectionResultsSummary summary = new ConvectionResultsSummary();
        double simDurationS = ConfigLoader.d(sweepConfig, "sim_duration_s", 604_800.0);
        double qWasteAvg = ConfigLoader.d(sweepConfig, "q_waste_avg_w", 39_523_125.0);
        double ambientTempC = ConfigLoader.d(sweepConfig, "ambient_temp_c", 22.0);
        double sourceTempC = ConfigLoader.d(sweepConfig, "source_temp_c", 45.0);
        double avgWasteMw = qWasteAvg / 1_000_000.0;

        ConvectionCaptureConfig refCfg = buildConfig(captureDefaults, Map.of());
        refCfg.chimneyHeightM = ConfigLoader.d(sweepConfig, "reference_chimney_height_m", refCfg.chimneyHeightM);
        refCfg.contactorAreaM2 = ConfigLoader.d(sweepConfig, "reference_contactor_area_m2", refCfg.contactorAreaM2);

        ConvectionSweepPoint ref = evaluatePoint(
                "reference", "Reference hall", refCfg, qWasteAvg, ambientTempC, sourceTempC, simDurationS, avgWasteMw
        );
        summary.setReferencePoint(ref);
        summary.addPoint(ref);

        List<Double> heights = doubleList(sweepConfig, "chimney_heights_m");
        for (double h : heights) {
            ConvectionCaptureConfig cfg = buildConfig(captureDefaults, Map.of("chimney_height_m", h));
            summary.addPoint(evaluatePoint(
                    "chimney_height", h + " m chimney", cfg, qWasteAvg, ambientTempC, sourceTempC,
                    simDurationS, avgWasteMw
            ));
        }

        List<Double> areas = doubleList(sweepConfig, "contactor_areas_m2");
        for (double area : areas) {
            ConvectionCaptureConfig cfg = buildConfig(captureDefaults, Map.of("contactor_area_m2", area));
            summary.addPoint(evaluatePoint(
                    "contactor_area", formatArea(area) + " contactor", cfg, qWasteAvg, ambientTempC, sourceTempC,
                    simDurationS, avgWasteMw
            ));
        }

        List<Double> fractions = doubleList(sweepConfig, "waste_heat_fractions");
        for (double frac : fractions) {
            ConvectionCaptureConfig cfg = buildConfig(captureDefaults, Map.of("waste_heat_to_air_fraction", frac));
            summary.addPoint(evaluatePoint(
                    "waste_fraction", (int) (frac * 100) + "% to air", cfg, qWasteAvg, ambientTempC, sourceTempC,
                    simDurationS, avgWasteMw
            ));
        }

        ConvectionCaptureConfig fanOnly = buildConfig(captureDefaults, Map.of("waste_heat_to_air_fraction", 0.0));
        var zeroDraft = ConvectionCapturePhysics.solve(fanOnly, qWasteAvg, ambientTempC);
        ConvectionCaptureConfig convCfg = buildConfig(captureDefaults, Map.of());
        var convDraft = ConvectionCapturePhysics.solve(convCfg, qWasteAvg, ambientTempC);
        summary.addPoint(new ConvectionSweepPoint(
                "fan_comparison",
                "Convection vs fan-only",
                convCfg.chimneyHeightM,
                convCfg.contactorAreaM2,
                convCfg.wasteHeatToAirFraction,
                avgWasteMw,
                convDraft.volumeFlowM3S(),
                convDraft.fanBaselineW() / 1_000_000.0,
                convDraft.fanResidualW() / 1_000_000.0,
                convDraft.fanSavedW() / 1_000_000.0,
                ConvectionCaptureCycle.evaluate(convCfg, convDraft, sourceTempC, simDurationS).annualizedGrossTonnes(),
                ConvectionCaptureCycle.evaluate(convCfg, convDraft, sourceTempC, simDurationS).annualizedNetTonnes(),
                convDraft.deltaTK(),
                convDraft.captureRateKgS()
        ));

        return summary;
    }

    private ConvectionSweepPoint evaluatePoint(
            String sweepId,
            String label,
            ConvectionCaptureConfig cfg,
            double qWasteW,
            double ambientTempC,
            double sourceTempC,
            double simDurationS,
            double avgWasteMw
    ) {
        var draft = ConvectionCapturePhysics.solve(cfg, qWasteW, ambientTempC);
        var cycle = ConvectionCaptureCycle.evaluate(cfg, draft, sourceTempC, simDurationS);
        return new ConvectionSweepPoint(
                sweepId,
                label,
                cfg.chimneyHeightM,
                cfg.contactorAreaM2,
                cfg.wasteHeatToAirFraction,
                avgWasteMw,
                draft.volumeFlowM3S(),
                draft.fanBaselineW() / 1_000_000.0,
                draft.fanResidualW() / 1_000_000.0,
                draft.fanSavedW() / 1_000_000.0,
                cycle.annualizedGrossTonnes(),
                cycle.annualizedNetTonnes(),
                draft.deltaTK(),
                draft.captureRateKgS()
        );
    }

    private static ConvectionCaptureConfig buildConfig(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> merged = new java.util.LinkedHashMap<>(defaults);
        Map<String, Object> pc = ConfigLoader.map(defaults, "passive_convection");
        Map<String, Object> pcCopy = new java.util.LinkedHashMap<>(pc);
        pcCopy.putAll(overrides);
        merged.put("passive_convection", pcCopy);
        ConvectionCaptureConfig cfg = ConvectionCaptureConfig.fromYaml(merged);
        cfg.enabled = true;
        cfg.dacMode = "convection_hybrid";
        return cfg;
    }

    private static List<Double> doubleList(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(o -> ((Number) o).doubleValue()).toList();
        }
        return List.of();
    }

    private static String formatArea(double area) {
        if (area >= 1000) {
            return String.format("%.0f m²", area);
        }
        return area + " m²";
    }
}
