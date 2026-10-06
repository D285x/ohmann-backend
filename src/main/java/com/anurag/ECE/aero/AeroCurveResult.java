package com.anurag.ECE.aero;

/**
 * A Mach-indexed drag coefficient curve returned by the aero-service.
 * {@code source} is {@code "shockflow"} when the GPU CFD solver actually ran,
 * or {@code "analytic_fallback"} when the service fell back to its built-in
 * empirical transonic drag model (no GPU / shockFLOW not installed there).
 */
public record AeroCurveResult(double[] mach, double[] cd, String source) {
}
