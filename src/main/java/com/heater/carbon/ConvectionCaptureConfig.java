package com.heater.carbon;

import com.heater.config.ConfigLoader;

import java.util.Map;

public final class ConvectionCaptureConfig {

    public boolean enabled;
    public String dacMode = "liquid";
    public double chimneyHeightM = 120.0;
    public double chimneyAreaM2 = 800.0;
    public double contactorAreaM2 = 50_000.0;
    public double bedResistancePaS2Kg2 = 0.002;
    public double wasteHeatToAirFraction = 0.35;
    public double captureEfficiency = 0.15;
    public double ambientCo2Ppm = 420.0;
    public double fanEfficiency = 0.65;
    public double airDensityKgM3 = 1.2;
    public double cpAir = 1005.0;
    public double gravity = 9.81;
    public double dischargeCoeff = 0.65;
    public double minDeltaTK = 2.0;
    public double adsorbHours = 8.0;
    public double regenHours = 2.0;
    public boolean reuseLiquidLoop = true;
    public double regenerationTemp = 90.0;
    public double minSourceTemp = 40.0;
    public double heatPumpCop = 3.2;
    public double specificHeatDutyJPerKg = 5_500_000.0;
    public double hpCapacityW = 8_100_000.0;
    public double gridCo2KgPerKwh = 0.39;

    public static ConvectionCaptureConfig fromYaml(Map<String, Object> root) {
        Map<String, Object> pc = ConfigLoader.map(root, "passive_convection");
        if (pc.isEmpty()) {
            return disabled();
        }
        ConvectionCaptureConfig c = new ConvectionCaptureConfig();
        c.enabled = pc.get("enabled") instanceof Boolean b && b;
        if (pc.get("dac_mode") instanceof String mode) {
            c.dacMode = mode;
        }
        c.chimneyHeightM = ConfigLoader.d(pc, "chimney_height_m", c.chimneyHeightM);
        c.chimneyAreaM2 = ConfigLoader.d(pc, "chimney_area_m2", c.chimneyAreaM2);
        c.contactorAreaM2 = ConfigLoader.d(pc, "contactor_area_m2", c.contactorAreaM2);
        c.bedResistancePaS2Kg2 = ConfigLoader.d(pc, "bed_resistance_pa_s2_kg2", c.bedResistancePaS2Kg2);
        c.wasteHeatToAirFraction = ConfigLoader.d(pc, "waste_heat_to_air_fraction", c.wasteHeatToAirFraction);
        c.captureEfficiency = ConfigLoader.d(pc, "capture_efficiency", c.captureEfficiency);
        c.ambientCo2Ppm = ConfigLoader.d(pc, "ambient_co2_ppm", c.ambientCo2Ppm);
        c.fanEfficiency = ConfigLoader.d(pc, "fan_efficiency", c.fanEfficiency);
        c.airDensityKgM3 = ConfigLoader.d(pc, "air_density_kg_m3", c.airDensityKgM3);
        c.cpAir = ConfigLoader.d(pc, "cp_air", c.cpAir);
        c.gravity = ConfigLoader.d(pc, "gravity", c.gravity);
        c.dischargeCoeff = ConfigLoader.d(pc, "discharge_coeff", c.dischargeCoeff);
        c.minDeltaTK = ConfigLoader.d(pc, "min_delta_t_k", c.minDeltaTK);

        Map<String, Object> cycle = ConfigLoader.map(pc, "cycle");
        c.adsorbHours = ConfigLoader.d(cycle, "adsorb_hours", c.adsorbHours);
        c.regenHours = ConfigLoader.d(cycle, "regen_hours", c.regenHours);

        Map<String, Object> regen = ConfigLoader.map(pc, "regen");
        if (regen.get("reuse_liquid_loop") instanceof Boolean b) {
            c.reuseLiquidLoop = b;
        }
        c.regenerationTemp = ConfigLoader.d(regen, "regeneration_temp", c.regenerationTemp);
        c.minSourceTemp = ConfigLoader.d(regen, "min_source_temp", c.minSourceTemp);
        c.heatPumpCop = ConfigLoader.d(regen, "heat_pump_cop", c.heatPumpCop);
        c.specificHeatDutyJPerKg = ConfigLoader.d(regen, "specific_heat_duty_j_per_kg", c.specificHeatDutyJPerKg);
        c.hpCapacityW = ConfigLoader.d(regen, "hp_capacity_w", c.hpCapacityW);

        Map<String, Object> climate = ConfigLoader.map(root, "climate");
        c.gridCo2KgPerKwh = ConfigLoader.d(climate, "grid_co2_kg_per_kwh", c.gridCo2KgPerKwh);
        return c;
    }

    public static ConvectionCaptureConfig disabled() {
        ConvectionCaptureConfig c = new ConvectionCaptureConfig();
        c.enabled = false;
        return c;
    }

    public boolean convectionHybrid() {
        return enabled && "convection_hybrid".equalsIgnoreCase(dacMode);
    }
}
