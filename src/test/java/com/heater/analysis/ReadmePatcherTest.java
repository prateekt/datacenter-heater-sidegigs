package com.heater.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReadmePatcherTest {

    @Test
    void patchesBetweenMarkers(@TempDir Path dir) throws Exception {
        Path readme = dir.resolve("README.md");
        Files.writeString(readme, """
                # Title

                <!-- SCALABILITY:BEGIN — auto-generated -->
                old content
                <!-- SCALABILITY:END -->

                footer
                """);

        ReadmePatcher.patch(readme, "## New section\n\nFresh numbers.");
        String updated = Files.readString(readme);
        assertTrue(updated.contains("Fresh numbers."));
        assertFalse(updated.contains("old content"));
        assertTrue(updated.contains("<!-- SCALABILITY:END -->"));
    }

    @Test
    void patchesConvectionBetweenMarkers(@TempDir Path dir) throws Exception {
        Path readme = dir.resolve("README.md");
        Files.writeString(readme, """
                # Title

                <!-- CONVECTION:BEGIN — auto-generated -->
                old convection
                <!-- CONVECTION:END -->
                """);

        ReadmePatcher.patchConvection(readme, "## Chimney DAC\n\nPlain English results.");
        String updated = Files.readString(readme);
        assertTrue(updated.contains("Plain English results."));
        assertFalse(updated.contains("old convection"));
        assertTrue(updated.contains("<!-- CONVECTION:END -->"));
    }
}
