package com.anurag.ECE.physics;

import java.util.List;

/**
 * Immutable vehicle description used by the simulator.
 * <p>
 * {@code curveMach}/{@code curveCd} are an optional Mach-indexed drag coefficient
 * table (ascending Mach, same length), produced by the shockFLOW aero-service
 * (see {@link com.anurag.ECE.aero.AeroClient}). When absent, {@link #dragCoefficient}
 * is used unchanged at every Mach number, exactly as before that feature existed.
 */
public record VehicleSpec(String name, List<StageSpec> stages, double diameterM, double dragCoefficient,
                           double[] curveMach, double[] curveCd) {

    /** Convenience constructor for a vehicle with a constant drag coefficient (no Mach table). */
    public VehicleSpec(String name, List<StageSpec> stages, double diameterM, double dragCoefficient) {
        this(name, stages, diameterM, dragCoefficient, null, null);
    }

    public double referenceArea() {
        return Math.PI * diameterM * diameterM / 4.0;
    }

    public double liftoffMass(double payloadKg) {
        return payloadKg + stages.stream().mapToDouble(s -> s.propellantKg() + s.dryKg()).sum();
    }

    public double liftoffThrustToWeight(double payloadKg) {
        return stages.get(0).thrustN() / (liftoffMass(payloadKg) * Constants.G0);
    }

    /** Ideal (Tsiolkovsky) delta-v of the complete stack. */
    public double idealDeltaV(double payloadKg) {
        double dv = 0;
        double mass = liftoffMass(payloadKg);
        for (StageSpec s : stages) {
            dv += s.exhaustVelocity() * Math.log(mass / (mass - s.propellantKg()));
            mass -= s.propellantKg() + s.dryKg();
        }
        return dv;
    }

    /**
     * Drag coefficient at the given Mach number: linearly interpolated from the
     * shockFLOW-derived curve when one is present (clamped beyond the table's
     * ends), otherwise the constant {@link #dragCoefficient}.
     */
    public double dragCoefficientAt(double mach) {
        if (curveMach == null || curveMach.length == 0) return dragCoefficient;
        int n = curveMach.length;
        if (mach <= curveMach[0]) return curveCd[0];
        if (mach >= curveMach[n - 1]) return curveCd[n - 1];
        for (int i = 1; i < n; i++) {
            if (mach <= curveMach[i]) {
                double t = (mach - curveMach[i - 1]) / (curveMach[i] - curveMach[i - 1]);
                return curveCd[i - 1] + t * (curveCd[i] - curveCd[i - 1]);
            }
        }
        return dragCoefficient;
    }
}
