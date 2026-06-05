package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionCaptureAnalyzerTest {

    @Test
    void sweepIncreasesAirflowWithChimneyHeight() throws Exception {
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(
                "config/convection_sweep.yaml",
                "config/passive_convection_capture.yaml"
        );
        ConvectionResultsSummary summary = analyzer.runAll();
        var heightPts = summary.bySweep("chimney_height");
        assertTrue(heightPts.size() >= 2);

        double prev = 0;
        for (ConvectionSweepPoint p : heightPts) {
            assertTrue(p.airflowM3S() >= prev, "airflow should grow with chimney height");
            prev = p.airflowM3S();
        }
    }

    @Test
    void sweepIncreasesCaptureWithContactorArea() throws Exception {
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(
                "config/convection_sweep.yaml",
                "config/passive_convection_capture.yaml"
        );
        ConvectionResultsSummary summary = analyzer.runAll();
        var areaPts = summary.bySweep("contactor_area");
        assertTrue(areaPts.size() >= 2);

        double prevCapture = 0;
        for (ConvectionSweepPoint p : areaPts) {
            assertTrue(p.grossCo2TonnesYr() >= prevCapture,
                    "gross capture should grow with contactor area: " + p.label());
            prevCapture = p.grossCo2TonnesYr();
        }
        ConvectionSweepPoint smallest = areaPts.get(0);
        ConvectionSweepPoint largest = areaPts.get(areaPts.size() - 1);
        assertTrue(largest.grossCo2TonnesYr() > smallest.grossCo2TonnesYr() * 1.5,
                "largest contactor should materially exceed smallest");
    }

    @Test
    void referencePointHasPositiveCapture() throws Exception {
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(
                "config/convection_sweep.yaml",
                "config/passive_convection_capture.yaml"
        );
        ConvectionResultsSummary summary = analyzer.runAll();
        assertNotNull(summary.referencePoint());
        assertTrue(summary.referencePoint().grossCo2TonnesYr() > 0);
        assertTrue(summary.referencePoint().fanSavedMw() > 0);
    }
}
