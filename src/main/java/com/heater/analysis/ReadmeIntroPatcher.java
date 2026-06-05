package com.heater.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the manual README intro (above auto-generated blocks) aligned with simulation JSON.
 */
public final class ReadmeIntroPatcher {

    private static final Pattern INTRO_PATTERN = Pattern.compile(
            "<!-- INTRO:BEGIN[^>]*-->.*?<!-- INTRO:END -->",
            Pattern.DOTALL
    );

    private ReadmeIntroPatcher() {}

    public static void patch(Path readmePath, ResultsSummary thermal, ConvectionResultsSummary convection)
            throws IOException {
        SummaryJsonReader.IntroSnapshot snap = snapshotFrom(thermal, convection);
        patch(readmePath, snap);
    }

    public static void patchFromJson(Path readmePath, Path thermalJson, Path convectionJson) throws IOException {
        patch(readmePath, SummaryJsonReader.loadIntro(thermalJson, convectionJson));
    }

    public static void patch(Path readmePath, SummaryJsonReader.IntroSnapshot snap) throws IOException {
        if (snap == null) {
            return;
        }
        String readme = Files.readString(readmePath);
        if (!readme.contains("<!-- INTRO:BEGIN")) {
            return;
        }
        String block = buildIntroBlock(snap);
        String replacement = "<!-- INTRO:BEGIN — synced from docs/results_summary.json; do not edit -->\n"
                + block.trim()
                + "\n<!-- INTRO:END -->";
        Matcher m = INTRO_PATTERN.matcher(readme);
        if (!m.find()) {
            throw new IllegalStateException("Could not match INTRO marker block in README");
        }
        Files.writeString(readmePath, m.replaceFirst(Matcher.quoteReplacement(replacement)));
    }

    static String buildIntroBlock(ResultsSummary thermal, ConvectionResultsSummary convection) {
        return buildIntroBlock(snapshotFrom(thermal, convection));
    }

    static String buildIntroBlock(SummaryJsonReader.IntroSnapshot snap) {
        return SummaryJsonReader.formatIntroMarkdown(snap);
    }

    private static SummaryJsonReader.IntroSnapshot snapshotFrom(
            ResultsSummary thermal, ConvectionResultsSummary convection
    ) {
        if (thermal == null) {
            return new SummaryJsonReader.IntroSnapshot(0, 0, 0, 0, 0, 0, 0, 0);
        }
        SweepPoint b200 = thermal.points().stream()
                .filter(p -> "gpu_generation".equals(p.sweepId()) && "B200_LC".equals(p.profileId()))
                .findFirst()
                .orElse(null);
        HeatApplicationPoint dac = thermal.applications().stream()
                .filter(a -> "dac_priority".equals(a.scenarioId()))
                .findFirst()
                .orElse(null);
        ConvectionSweepPoint convRef = convection != null ? convection.referencePoint() : null;

        double wasteMw = b200 != null ? b200.avgWasteHeatMw() : 0;
        double recoveredGwh = b200 != null ? b200.thermal().annualizedRecoveredGwh() : 0;
        double netTonnes = b200 != null ? b200.annualizedNetTonnes() : 0;
        double showers = dac != null ? dac.hotShowersEquivalent() : 0;
        double chimney = convRef != null ? convRef.chimneyHeightM() : 0;
        double airflow = convRef != null ? convRef.airflowM3S() : 0;
        double fanSaved = convRef != null ? convRef.fanSavedMw() : 0;
        double convNet = convRef != null ? convRef.netCo2TonnesYr() : 0;

        return new SummaryJsonReader.IntroSnapshot(
                wasteMw, recoveredGwh, netTonnes, showers, chimney, airflow, fanSaved, convNet);
    }
}
