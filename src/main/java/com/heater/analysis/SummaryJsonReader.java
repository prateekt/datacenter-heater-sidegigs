package com.heater.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal reader for {@code docs/results_summary.json} — enough to sync README intro text.
 */
public final class SummaryJsonReader {

    private static final Pattern APP_BLOCK = Pattern.compile(
            "\\{\\s*\"scenario_id\"\\s*:\\s*\"dac_priority\"[^}]*\\}",
            Pattern.DOTALL
    );
    private static final Pattern B200_BLOCK = Pattern.compile(
            "\\{\\s*\"sweep_id\"\\s*:\\s*\"gpu_generation\"[^}]*\"profile_id\"\\s*:\\s*\"B200_LC\"[^}]*\\}",
            Pattern.DOTALL
    );
    private static final Pattern CONV_REF_BLOCK = Pattern.compile(
            "\\{\\s*\"sweep_id\"\\s*:\\s*\"reference\"[^}]*\\}",
            Pattern.DOTALL
    );
    private SummaryJsonReader() {}

    public record IntroSnapshot(
            double avgWasteHeatMw,
            double recoveredGwh,
            double netTonnes,
            double hotShowersEquiv,
            double chimneyHeightM,
            double airflowM3S,
            double fanSavedMw,
            double netCo2TonnesYr
    ) {
        boolean hasThermal() {
            return avgWasteHeatMw > 0 && recoveredGwh > 0;
        }

        boolean hasConvection() {
            return airflowM3S > 0;
        }
    }

    public static IntroSnapshot loadIntro(Path thermalJson, Path convectionJson) throws IOException {
        String thermal = thermalJson != null && Files.exists(thermalJson)
                ? Files.readString(thermalJson) : "";
        String convection = convectionJson != null && Files.exists(convectionJson)
                ? Files.readString(convectionJson) : "";
        return parse(thermal, convection);
    }

    static IntroSnapshot parse(String thermalJson, String convectionJson) {
        double showers = 0;
        double netTonnes = 0;
        double wasteMw = 0;
        double recoveredGwh = 0;

        Matcher app = APP_BLOCK.matcher(thermalJson);
        if (app.find()) {
            String block = app.group();
            showers = field(block, "hot_showers_equiv");
            netTonnes = field(block, "net_co2e_tonnes_per_year");
        }

        Matcher b200 = B200_BLOCK.matcher(thermalJson);
        if (b200.find()) {
            String block = b200.group();
            wasteMw = field(block, "avg_waste_heat_mw");
            recoveredGwh = field(block, "annualized_recovered_gwh");
            if (netTonnes == 0) {
                netTonnes = field(block, "annualized_net_tonnes");
            }
        }

        double chimney = 0;
        double airflow = 0;
        double fanSaved = 0;
        double convNet = 0;
        Matcher ref = CONV_REF_BLOCK.matcher(convectionJson);
        if (ref.find()) {
            String block = ref.group();
            chimney = field(block, "chimney_height_m");
            airflow = field(block, "airflow_m3_s");
            fanSaved = field(block, "fan_saved_mw");
            convNet = field(block, "net_co2_tonnes_yr");
        }

        return new IntroSnapshot(wasteMw, recoveredGwh, netTonnes, showers, chimney, airflow, fanSaved, convNet);
    }

    private static double field(String block, String key) {
        Matcher m = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+)"
        ).matcher(block);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    public static String formatIntroMarkdown(IntroSnapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Side gig results (NVIDIA U.S.)\n\n");
        if (snap.hasThermal()) {
            sb.append(String.format(Locale.US,
                    "**TL;DR:** One **25,000-GPU** hall's exhaust could run a serious **side gig** — "
                            + "**~%.1f MW** of waste heat delivering **~%.1f GWh/yr** of usable thermal service, "
                            + "enough for **%s** or colocated DAC/algae. "
                            + "*Grid scenario:* ~%,.0f tonnes CO₂/yr net removed.\n\n",
                    snap.avgWasteHeatMw(),
                    snap.recoveredGwh(),
                    HeatApplicationAnalyzer.formatHotShowers(snap.hotShowersEquiv()),
                    snap.netTonnes()));
        } else {
            sb.append("**TL;DR:** One **25,000-GPU** hall's exhaust could run a serious **side gig** — "
                    + "see [Full analysis](#scalability-charts) for simulator numbers.\n\n");
        }

        sb.append("Full thermal charts, downstream trade-offs, and the grid-dependent carbon appendix are in ")
                .append("[Full analysis](#scalability-charts) below. Auto-generated when you run `./gradlew generateFigures`.\n\n");
        sb.append("For a balanced DAC + algae rotation (one pipe at a time), see [balanced run](#balanced-dac--algae) ")
                .append("in [Try it yourself](#try-it-yourself).\n\n");

        if (snap.hasConvection()) {
            sb.append(String.format(Locale.US,
                    "**Speculative chimney DAC (reference hall, %.0f m tower):** ~**%.0f m³/s** natural draft, "
                            + "~**%.2f MW** fan electricity saved, ~**%.0f tonnes CO₂/yr net** in the grid scenario — "
                            + "see [full plain-English breakdown](#convection-speculative).\n",
                    snap.chimneyHeightM(),
                    snap.airflowM3S(),
                    snap.fanSavedMw(),
                    snap.netCo2TonnesYr()));
        } else {
            sb.append("**Speculative chimney DAC:** see [Chimney DAC](#convection-speculative) "
                    + "(run `./gradlew generateConvectionFigures`).\n");
        }
        return sb.toString();
    }
}
