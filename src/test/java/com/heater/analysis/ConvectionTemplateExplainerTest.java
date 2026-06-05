package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionTemplateExplainerTest {

    @Test
    void explainIncludesKindergartenSections() throws Exception {
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(
                "config/convection_sweep.yaml",
                "config/passive_convection_capture.yaml"
        );
        ConvectionResultsSummary summary = analyzer.runAll();
        ConvectionAnalogies analogies = ConvectionAnalogies.load("config/convection_analogies.yaml");
        String markdown = ConvectionTemplateExplainer.explain(summary, analogies);

        assertTrue(markdown.contains("In one sentence"));
        assertTrue(markdown.contains("Picture this"));
        assertTrue(markdown.contains("plain English"));
        assertTrue(markdown.contains("Honest limits"));
        assertTrue(markdown.toLowerCase().contains("speculative"));

        var validation = ConvectionExplainerValidator.validate(markdown, summary);
        assertTrue(validation.warnings().isEmpty(), () -> String.join(", ", validation.warnings()));
    }
}
