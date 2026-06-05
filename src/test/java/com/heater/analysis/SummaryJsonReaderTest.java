package com.heater.analysis;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SummaryJsonReaderTest {

    @Test
    void parsesB200AndDacPriorityFromResultsJson() throws Exception {
        Path path = Path.of("docs/results_summary.json");
        if (!Files.exists(path)) {
            return;
        }
        SummaryJsonReader.IntroSnapshot snap = SummaryJsonReader.loadIntro(path, null);
        assertTrue(snap.hasThermal());
        assertEquals(33.75, snap.avgWasteHeatMw(), 0.5);
        assertTrue(snap.recoveredGwh() > 70.0 && snap.recoveredGwh() < 72.0);
        assertTrue(snap.netTonnes() > 37_000);
        assertTrue(snap.hotShowersEquiv() > 28_000_000);
    }

    @Test
    void parsesConvectionReferenceFromSummaryJson() throws Exception {
        Path path = Path.of("docs/convection_summary.json");
        if (!Files.exists(path)) {
            return;
        }
        SummaryJsonReader.IntroSnapshot snap = SummaryJsonReader.loadIntro(null, path);
        assertTrue(snap.hasConvection());
        assertTrue(snap.airflowM3S() > 100);
        assertTrue(snap.netCo2TonnesYr() > 0);
    }
}
