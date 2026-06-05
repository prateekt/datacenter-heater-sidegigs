package com.heater.analysis;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConvectionChartGenerator {

    private static final int W = 1000;
    private static final int H = 580;

    private final Path outputDir;

    public ConvectionChartGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> generateAll(ConvectionResultsSummary summary) throws IOException {
        Files.createDirectories(outputDir);
        List<String> paths = new ArrayList<>();
        paths.add(writeAirflowVsHeight(summary));
        paths.add(writeCo2VsHeight(summary));
        paths.add(writeFanSavings(summary));
        for (String p : paths) {
            summary.addChart(p);
        }
        return paths;
    }

    private String writeAirflowVsHeight(ConvectionResultsSummary summary) throws IOException {
        List<ConvectionSweepPoint> pts = summary.bySweep("chimney_height");
        List<Double> x = pts.stream().map(ConvectionSweepPoint::chimneyHeightM).toList();
        List<Double> y = pts.stream().map(ConvectionSweepPoint::airflowM3S).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Natural Draft Airflow vs. Chimney Height (speculative)")
                .xAxisTitle("Chimney height (m)")
                .yAxisTitle("Airflow through contactors (m³/s)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.TEAL);
        chart.addSeries("airflow", x, y);
        return save(chart, "convection_airflow_vs_height.png");
    }

    private String writeCo2VsHeight(ConvectionResultsSummary summary) throws IOException {
        List<ConvectionSweepPoint> pts = summary.bySweep("chimney_height");
        List<Double> x = pts.stream().map(ConvectionSweepPoint::chimneyHeightM).toList();
        List<Double> gross = pts.stream().map(ConvectionSweepPoint::grossCo2TonnesYr).toList();
        List<Double> net = pts.stream().map(ConvectionSweepPoint::netCo2TonnesYr).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("CO₂ Capture vs. Chimney Height (speculative)")
                .xAxisTitle("Chimney height (m)")
                .yAxisTitle("CO₂ (metric tonnes / year)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.GREEN);
        chart.addSeries("gross captured", x, gross);
        chart.addSeries("net (after fan + regen grid penalty)", x, net);
        return save(chart, "convection_co2_vs_height.png");
    }

    private String writeFanSavings(ConvectionResultsSummary summary) throws IOException {
        ConvectionSweepPoint ref = summary.referencePoint();
        if (ref == null) {
            return saveEmptyPlaceholder();
        }
        List<String> labels = List.of("Fan-only baseline", "Convection-assisted");
        List<Double> fanMw = List.of(ref.fanBaselineMw(), ref.fanResidualMw());
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Fan Power: Baseline vs. Convection-Assisted (reference hall)")
                .xAxisTitle("Mode")
                .yAxisTitle("Fan electricity (MW)")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.INDIGO);
        chart.addSeries("fan power", labels, fanMw);
        return save(chart, "convection_fan_savings.png");
    }

    private String save(XYChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String save(CategoryChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String saveEmptyPlaceholder() {
        return "docs/figures/convection_fan_savings.png";
    }
}
