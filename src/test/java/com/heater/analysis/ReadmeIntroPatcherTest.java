package com.heater.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReadmeIntroPatcherTest {

    @Test
    void patchesIntroBlockFromJson(@TempDir Path dir) throws Exception {
        Path readme = dir.resolve("README.md");
        Files.writeString(readme, """
                # Title

                <!-- INTRO:BEGIN -->
                stale
                <!-- INTRO:END -->

                <!-- SCALABILITY:BEGIN -->
                body
                <!-- SCALABILITY:END -->
                """);

        Path thermal = dir.resolve("thermal.json");
        Files.writeString(thermal, Files.readString(Path.of("docs/results_summary.json")));

        ReadmeIntroPatcher.patchFromJson(readme, thermal, null);

        String updated = Files.readString(readme);
        assertTrue(updated.contains("33.7") || updated.contains("33.8"),
                "intro should use simulator MW not stale rounding");
        assertTrue(updated.contains("70.9") || updated.contains("71.0"));
        assertFalse(updated.contains("stale"));
    }
}
