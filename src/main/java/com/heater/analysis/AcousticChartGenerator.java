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

public final class AcousticChartGenerator {

    private static final int W = 1000;
    private static final int H = 580;

    private final Path outputDir;

    public AcousticChartGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> generateAll(AcousticResultsSummary summary) throws IOException {
        Files.createDirectories(outputDir);
        List<String> paths = new ArrayList<>();
        paths.add(writeSplVsLinerDepth(summary));
        paths.add(writeSoundscapeVsWaterFlow(summary));
        paths.add(writeMdmgVsSteps(summary));
        paths.add(writeCouplingComparison(summary));
        paths.add(writeOrchestraInstrumentedFraction(summary));
        paths.add(writeOrchestraRackCount(summary));
        paths.addAll(writeOrchestraBowCovers(summary));
        for (String p : paths) {
            summary.addChart(p);
        }
        return paths;
    }

    private String writeSplVsLinerDepth(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("liner_depth");
        List<Double> x = pts.stream().map(AcousticSweepPoint::linerDepthMm).toList();
        List<Double> baseline = pts.stream().map(AcousticSweepPoint::baselineDba).toList();
        List<Double> fence = pts.stream().map(AcousticSweepPoint::fenceLineDba).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Fence-line SPL vs. Metamaterial Liner Depth (MSE)")
                .xAxisTitle("Liner depth (mm)")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.TEAL);
        chart.addSeries("baseline fan noise", x, baseline);
        chart.addSeries("after MSE", x, fence);
        return save(chart, "acoustic_spl_vs_liner_depth.png");
    }

    private String writeSoundscapeVsWaterFlow(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("water_flow");
        List<Double> x = pts.stream().map(AcousticSweepPoint::waterFlowLS).toList();
        List<Double> sqi = pts.stream().map(AcousticSweepPoint::soundscapeQualityIndex).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Soundscape Quality Index vs. Water Feature Flow")
                .xAxisTitle("Water flow (L/s)")
                .yAxisTitle("Soundscape quality index (0–1)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.GREEN);
        chart.addSeries("SQI", x, sqi);
        return save(chart, "acoustic_sqi_vs_water_flow.png");
    }

    private String writeMdmgVsSteps(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("diffusion_steps");
        List<Double> x = pts.stream().map(p -> (double) p.diffusionSteps()).toList();
        List<Double> dist = pts.stream().map(AcousticSweepPoint::spectralDistance).toList();
        List<Double> harm = pts.stream().map(AcousticSweepPoint::harmonicity).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Mechanical Diffusion: Template Distance vs. Reverse Steps")
                .xAxisTitle("Reverse denoising steps")
                .yAxisTitle("Metric value")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.TEAL);
        chart.addSeries("spectral distance to template", x, dist);
        chart.addSeries("harmonicity", x, harm);
        return save(chart, "acoustic_mdmg_vs_steps.png");
    }

    private String writeCouplingComparison(AcousticResultsSummary summary) throws IOException {
        var runs = summary.referenceRuns();
        if (runs == null) {
            return saveEmptyPlaceholder();
        }
        List<String> labels = List.of("Baseline", "MSE decoupled", "MSE chimney-coupled");
        List<Double> dba = List.of(runs.baselineDba(), runs.mseDecoupledDba(), runs.mseCoupledDba());
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Fence-line SPL: Baseline vs. MSE Modes")
                .xAxisTitle("Mode")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.addSeries("dBA", labels, dba);
        return saveCategory(chart, "acoustic_coupling_comparison.png");
    }

    private String writeOrchestraInstrumentedFraction(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("instrumented_fraction");
        if (pts.isEmpty()) return saveEmptyOrchestraPlaceholder("instrumented_fraction");
        List<Double> x = new ArrayList<>();
        List<Double> tonal = new ArrayList<>();
        List<Double> fence = new ArrayList<>();
        for (AcousticSweepPoint p : pts) {
            String label = p.label();
            int pct = label.indexOf('%');
            if (pct > 0) {
                x.add(Double.parseDouble(label.substring(0, pct).trim()));
            }
            tonal.add(p.tonalProminenceDb());
            fence.add(p.fenceLineDba());
        }
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Fan Orchestra: Tonal Prominence vs. Instrumented Fraction")
                .xAxisTitle("Fans with bow cells (%)")
                .yAxisTitle("Metric value")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.INDIGO, ChartStyle.TEAL);
        chart.addSeries("tonal prominence (dB)", x, tonal);
        chart.addSeries("fence-line SPL (dBA)", x, fence);
        return save(chart, "acoustic_orchestra_instrumented_fraction.png");
    }

    private String writeOrchestraRackCount(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("rack_count");
        if (pts.isEmpty()) return saveEmptyOrchestraPlaceholder("rack_count");
        List<Double> racks = new ArrayList<>();
        List<Double> fence = new ArrayList<>();
        List<Double> baseline = new ArrayList<>();
        for (AcousticSweepPoint p : pts) {
            String label = p.label();
            int space = label.indexOf(' ');
            if (space > 0) {
                racks.add(Double.parseDouble(label.substring(0, space).trim()));
            }
            fence.add(p.fenceLineDba());
            baseline.add(p.baselineDba());
        }
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Hall Scale: Fence-line SPL vs. Rack Count (15k instruments at 2500 racks)")
                .xAxisTitle("Rack count")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.TEAL);
        chart.addSeries("baseline fan noise", racks, baseline);
        chart.addSeries("MSE + fan orchestra", racks, fence);
        return save(chart, "acoustic_orchestra_rack_count.png");
    }

    private List<String> writeOrchestraBowCovers(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("bow_cover");
        if (pts.isEmpty()) {
            return List.of(saveEmptyOrchestraPlaceholder("bow_cover"));
        }
        List<String> labels = pts.stream().map(p -> p.label().replace(" cover", "")).toList();
        List<Double> tonal = pts.stream().map(AcousticSweepPoint::tonalProminenceDb).toList();
        List<Double> fence = pts.stream().map(AcousticSweepPoint::fenceLineDba).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Pickaso Cover Presets: Tonal Character at Fence Line")
                .xAxisTitle("Bow wheel cover (simulated)")
                .yAxisTitle("Tonal prominence (dB)")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.INDIGO);
        chart.addSeries("tonal prominence", labels, tonal);
        CategoryChart fenceChart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Pickaso Cover Presets: Fence-line SPL")
                .xAxisTitle("Bow wheel cover (simulated)")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyCategory(fenceChart, ChartStyle.TEAL);
        fenceChart.addSeries("fence SPL", labels, fence);
        return List.of(
                saveCategory(chart, "acoustic_orchestra_bow_covers.png"),
                saveCategory(fenceChart, "acoustic_orchestra_bow_covers_spl.png")
        );
    }

    private String saveEmptyOrchestraPlaceholder(String name) throws IOException {
        XYChart chart = new XYChartBuilder().width(W).height(H).title("No " + name + " sweep data").build();
        return save(chart, "acoustic_orchestra_" + name + ".png");
    }

    private String save(XYChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String saveCategory(CategoryChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String saveEmptyPlaceholder() throws IOException {
        XYChart chart = new XYChartBuilder().width(W).height(H).title("No coupling data").build();
        return save(chart, "acoustic_coupling_comparison.png");
    }
}
