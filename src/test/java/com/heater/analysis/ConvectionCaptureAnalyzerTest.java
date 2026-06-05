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
