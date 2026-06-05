package com.heater.analysis;

import org.junit.jupiter.api.Test;

class MdmgBenchmarkIntegrationTest {

    @Test
    void generateMdmgBenchmarkCompletesWithoutReadmePatch() throws Exception {
        MdmgBenchmarkMain.main(new String[] {"--no-readme", "--skip-train"});
    }
}
