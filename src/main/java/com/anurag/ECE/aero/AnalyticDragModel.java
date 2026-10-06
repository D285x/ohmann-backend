package com.anurag.ECE.aero;

/**
 * Java port of the aero-service's analytic fallback ({@code aero-service/fallback.py}).
 * <p>
 * Used by the backend itself when the aero-service is not deployed or cannot be
 * reached, so "refine aerodynamics" still produces a Mach-indexed drag curve.
 * It is the same engineering correlation as the Python version: a constant
 * subsonic coefficient, a transonic drag-divergence bump centred on Mach 1, and
 * a supersonic wave-drag term decaying like 1/sqrt(M^2 - 1), scaled by how
 * slender the nose is. It is not a CFD result.
 */
public final class AnalyticDragModel {

    public static final String SOURCE = "analytic_fallback";

    private AnalyticDragModel() {
    }

    public static AeroCurveResult dragCurve(double finenessRatio, double machMin, double machMax, int points) {
        if (points < 2) points = 2;
        if (machMax <= machMin) machMax = machMin + 0.1;
        double cd0 = baseCd(finenessRatio);
        double bump = 0.55 / Math.max(1.0, finenessRatio / 2.0);

        double[] mach = new double[points];
        double[] cd = new double[points];
        for (int i = 0; i < points; i++) {
            mach[i] = machMin + (machMax - machMin) * i / (points - 1);
            cd[i] = cdAt(mach[i], cd0, bump);
        }
        return new AeroCurveResult(mach, cd, SOURCE);
    }

    static double baseCd(double finenessRatio) {
        return Math.max(0.15, 0.30 - 0.02 * finenessRatio);
    }

    static double cdAt(double m, double cd0, double bump) {
        if (m < 0.8) {
            return cd0;
        }
        if (m <= 1.2) {
            // Smooth rise-and-partial-fall bump peaking at Mach 1 (drag divergence)
            return cd0 + bump * Math.sin(Math.PI * (m - 0.8) / 0.8);
        }
        // Supersonic: wave drag decays roughly like the Prandtl-Glauert factor
        double decay = 1.0 / Math.sqrt(Math.max(m * m - 1.0, 0.05));
        double peak = cd0 + bump;
        return cd0 + (peak - cd0) * decay / Math.sqrt(1.2 * 1.2 - 1.0 + 0.05);
    }
}
