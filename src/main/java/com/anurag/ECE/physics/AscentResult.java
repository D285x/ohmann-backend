package com.anurag.ECE.physics;

import java.util.List;

/**
 * Outcome of one ascent simulation.
 * If {@code success} is false, {@code failureReason} explains why and {@code apoapsisShortfallM}
 * is used by the optimizer to steer towards feasible solutions.
 */
public record AscentResult(
        boolean success,
        String failureReason,
        double apoapsisShortfallM,
        Guidance guidance,
        double cutoffTimeS,
        double cutoffAltitudeM,
        double apoapsisAltM,
        double periapsisAltM,
        double circularizationDv,
        double availableDvAfterCutoff,
        double gravityLoss,
        double dragLoss,
        double steeringLoss,
        double burnedDv,
        double maxQ,
        List<TrajectoryPoint> trajectory) {

    /** Delta-v margin after circularization and any extra manoeuvre. */
    public double margin(double extraDv) {
        return availableDvAfterCutoff - circularizationDv - extraDv;
    }
}
