package com.anurag.ECE.physics;

/** One sample of the simulated ascent, for plotting and CSV export. */
public record TrajectoryPoint(double timeS, double altitudeKm, double downrangeKm, double velocityMs,
                              double flightPathDeg, double pitchDeg, double massKg,
                              double dynamicPressureKPa, int stage) {
}
