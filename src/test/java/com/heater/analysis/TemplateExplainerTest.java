package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateExplainerTest {

    @Test
    void conclusionIncludesVerdictAndWorthItGuide() throws Exception {
        SweepRunner runner = new SweepRunner(
                "config/nvidia_us_expansion.yaml",
                "src/test/resources/scalability_sweep_fast.yaml");
        ResultsSummary summary = runner.runAll();
        String markdown = TemplateExplainer.explain(summary, "config/gpu_profiles.yaml");

        assertTrue(markdown.contains("### Conclusion — significance, limits, and what's worth it"));
        assertTrue(markdown.contains("**Verdict:**"));
        assertTrue(markdown.contains("What is significant"));
        assertTrue(markdown.contains("What is not significant"));
        assertTrue(markdown.contains("What's worth it?"));
        assertFalse(markdown.contains("0.00% of U.S. emissions"));
    }
}
