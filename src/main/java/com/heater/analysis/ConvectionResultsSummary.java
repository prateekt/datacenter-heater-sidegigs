package com.heater.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ConvectionResultsSummary {

    private final String generatedAt;
    private final List<ConvectionSweepPoint> points = new ArrayList<>();
    private final List<String> chartPaths = new ArrayList<>();
    private ConvectionSweepPoint referencePoint;

    public ConvectionResultsSummary() {
        this.generatedAt = Instant.now().toString();
    }

    public void addPoint(ConvectionSweepPoint point) {
        points.add(point);
    }

    public void setReferencePoint(ConvectionSweepPoint point) {
        this.referencePoint = point;
    }

    public void addChart(String path) {
        chartPaths.add(path);
    }

    public List<ConvectionSweepPoint> points() {
        return List.copyOf(points);
    }

    public List<String> chartPaths() {
        return List.copyOf(chartPaths);
    }

    public ConvectionSweepPoint referencePoint() {
        return referencePoint;
    }

    public String generatedAt() {
        return generatedAt;
    }

    public List<ConvectionSweepPoint> bySweep(String sweepId) {
        return points.stream().filter(p -> p.sweepId().equals(sweepId)).toList();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"generated_at\": \"").append(generatedAt).append("\",\n");
        sb.append("  \"charts\": [");
        for (int i = 0; i < chartPaths.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(chartPaths.get(i)).append("\"");
        }
        sb.append("],\n");
        sb.append("  \"points\": [\n");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",\n");
            ConvectionSweepPoint p = points.get(i);
            sb.append("    {");
            sb.append("\"sweep_id\":\"").append(p.sweepId()).append("\",");
            sb.append("\"label\":\"").append(escape(p.label())).append("\",");
            sb.append("\"chimney_height_m\":").append(p.chimneyHeightM()).append(",");
            sb.append("\"contactor_area_m2\":").append(p.contactorAreaM2()).append(",");
            sb.append("\"waste_heat_to_air_fraction\":").append(p.wasteHeatToAirFraction()).append(",");
            sb.append("\"avg_waste_heat_mw\":").append(fmt(p.avgWasteHeatMw())).append(",");
            sb.append("\"airflow_m3_s\":").append(fmt(p.airflowM3S())).append(",");
            sb.append("\"fan_saved_mw\":").append(fmt(p.fanSavedMw())).append(",");
            sb.append("\"gross_co2_tonnes_yr\":").append(fmt(p.grossCo2TonnesYr())).append(",");
            sb.append("\"net_co2_tonnes_yr\":").append(fmt(p.netCo2TonnesYr())).append(",");
            sb.append("\"delta_t_k\":").append(fmt(p.deltaTK())).append(",");
            sb.append("\"capture_rate_kg_s\":").append(fmt(p.captureRateKgS()));
            sb.append("}");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }
}
