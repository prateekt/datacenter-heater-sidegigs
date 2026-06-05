package com.heater.thermal;

import java.util.List;
import java.util.Map;

public record ScenarioProfile(
        String name,
        double duration,
        double qWasteBase,
        double qWastePeak,
        List<Map<String, Object>> outsideTemps
) {}
