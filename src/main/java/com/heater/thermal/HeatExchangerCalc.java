package com.heater.thermal;

public final class HeatExchangerCalc {

    private HeatExchangerCalc() {}

    public record HxResult(double qTransferW, double tPrimaryOut, double tSecondaryOut) {}

    public static HxResult compute(
            double tPrimaryIn,
            double tSecondaryIn,
            double mdotPrimary,
            double mdotSecondary,
            double ua,
            double cp
    ) {
        if (mdotPrimary <= 0 || mdotSecondary <= 0) {
            return new HxResult(0.0, tPrimaryIn, tSecondaryIn);
        }

        double cPrimary = mdotPrimary * cp;
        double cSecondary = mdotSecondary * cp;
        double cMin = Math.min(cPrimary, cSecondary);
        double cMax = Math.max(cPrimary, cSecondary);
        double cr = cMax > 0 ? cMin / cMax : 0.0;

        double ntu = cMin > 0 ? ua / cMin : 0.0;
        double eps;
        if (cr < 1.0 - 1e-9) {
            eps = (1.0 - cr * (1.0 - Math.exp(-ntu * (1.0 - cr)))) / (1.0 - cr);
        } else {
            eps = ntu / (1.0 + ntu);
        }
        eps = Math.max(0.0, Math.min(1.0, eps));

        double qMax = cMin * (tPrimaryIn - tSecondaryIn);
        double q = tPrimaryIn > tSecondaryIn ? eps * qMax : 0.0;

        double tPrimaryOut = tPrimaryIn - q / cPrimary;
        double tSecondaryOut = tSecondaryIn + q / cSecondary;
        return new HxResult(q, tPrimaryOut, tSecondaryOut);
    }
}
