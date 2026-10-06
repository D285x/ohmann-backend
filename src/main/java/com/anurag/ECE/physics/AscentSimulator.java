package com.anurag.ECE.physics;

import java.util.ArrayList;
import java.util.List;

import static com.anurag.ECE.physics.Constants.*;

/**
 * Planar point-mass ascent simulator over a spherical Earth.
 * <p>
 * State is expressed in polar coordinates in the (inertial) trajectory plane:
 * radius r, downrange angle theta, radial velocity vr and transverse velocity vt.
 * The atmosphere co-rotates with the Earth; the in-plane component of the surface
 * rotation speed is supplied by the caller (it depends on latitude and azimuth).
 * Integration uses fixed-step 4th-order Runge-Kutta with step refinement at
 * staging and at engine cutoff.
 */
public class AscentSimulator {

    private static final double DT = 0.5;
    private static final double MAX_TIME = 4000;
    private static final double MIN_CUTOFF_ALTITUDE = 80_000;
    private static final double SAMPLE_EVERY_S = 5.0;

    private final VehicleSpec vehicle;
    private final double payloadKg;
    private final double targetApoapsisR;
    private final double surfaceRotationSpeed;

    /**
     * @param targetApoapsisAltM   apoapsis altitude at which the engine is cut off (m)
     * @param surfaceRotationSpeed in-plane Earth-rotation velocity at the pad (m/s, may be negative)
     */
    public AscentSimulator(VehicleSpec vehicle, double payloadKg, double targetApoapsisAltM, double surfaceRotationSpeed) {
        this.vehicle = vehicle;
        this.payloadKg = payloadKg;
        this.targetApoapsisR = R_EARTH + targetApoapsisAltM;
        this.surfaceRotationSpeed = surfaceRotationSpeed;
    }

    /** Mutable integration state. */
    private static final class State {
        double r, theta, vr, vt, m;

        State(double r, double theta, double vr, double vt, double m) {
            this.r = r;
            this.theta = theta;
            this.vr = vr;
            this.vt = vt;
            this.m = m;
        }

        State copy() {
            return new State(r, theta, vr, vt, m);
        }
    }

    public AscentResult simulate(Guidance g, boolean recordTrajectory) {
        List<StageSpec> stages = vehicle.stages();
        List<TrajectoryPoint> points = recordTrajectory ? new ArrayList<>() : List.of();

        if (vehicle.liftoffThrustToWeight(payloadKg) < 1.05) {
            return fail("Liftoff thrust-to-weight below 1.05", targetApoapsisR - R_EARTH, g, points);
        }

        State s = new State(R_EARTH, 0, 0, surfaceRotationSpeed, vehicle.liftoffMass(payloadKg));
        int stageIdx = 0;
        double propLeft = stages.get(0).propellantKg();
        double t = 0;
        double upperStart = -1, upperPsi0 = 0, upperDuration = 1;
        double gravLoss = 0, dragLoss = 0, steerLoss = 0, burned = 0, maxQ = 0;
        double bestApo = 0;
        double nextSample = 0;

        while (t < MAX_TIME) {
            StageSpec st = stages.get(stageIdx);
            if (stageIdx >= 1 && upperStart < 0) {
                upperStart = t;
                upperPsi0 = relativeFlightPath(s);
                upperDuration = 0;
                for (int k = stageIdx; k < stages.size(); k++) upperDuration += stages.get(k).burnTime();
            }

            double psi = pitch(g, t, stageIdx, s, upperStart, upperPsi0, upperDuration);
            if (recordTrajectory && t >= nextSample) {
                points.add(sample(t, s, psi, stageIdx + 1));
                nextSample += SAMPLE_EVERY_S;
            }

            double mdot = st.massFlow();
            double dt = Math.min(DT, propLeft / mdot);
            boolean aboveMinAlt = s.r - R_EARTH >= MIN_CUTOFF_ALTITUDE && stageIdx == stages.size() - 1;

            if (aboveMinAlt && Orbit.of(s).apoapsisR() >= targetApoapsisR) {
                // apoapsis already at/above target (overshoot while still in the atmosphere): cut off now
                return insertion(s, t, psi, stageIdx, propLeft, g, gravLoss, dragLoss, steerLoss, burned, maxQ,
                        points, recordTrajectory);
            }

            boolean cutoff = false;
            State next = rk4(s, dt, st, psi);
            if (aboveMinAlt && Orbit.of(next).apoapsisR() >= targetApoapsisR) {
                // refine the step so that cutoff lands exactly on the target apoapsis
                double lo = 0, hi = dt;
                for (int i = 0; i < 30; i++) {
                    double mid = 0.5 * (lo + hi);
                    if (Orbit.of(rk4(s, mid, st, psi)).apoapsisR() >= targetApoapsisR) hi = mid;
                    else lo = mid;
                }
                dt = hi;
                next = rk4(s, dt, st, psi);
                cutoff = true;
            }

            // loss bookkeeping (evaluated at the start of the step)
            double gamma = Math.atan2(s.vr, s.vt);
            double[] drag = drag(s);
            double q = dynamicPressure(s);
            maxQ = Math.max(maxQ, q);
            gravLoss += MU_EARTH / (s.r * s.r) * Math.sin(gamma) * dt;
            dragLoss += Math.hypot(drag[0], drag[1]) / s.m * dt;
            double thrustAcc = st.thrustN() / s.m;
            steerLoss += thrustAcc * (1 - Math.cos(Math.toRadians(psi) - gamma)) * dt;
            burned += st.exhaustVelocity() * Math.log(s.m / next.m);

            s = next;
            t += dt;
            propLeft -= mdot * dt;

            if (s.r < R_EARTH - 1) {
                return fail("Vehicle impacted the ground at t=" + Math.round(t) + " s",
                        targetApoapsisR - bestApo, g, points);
            }
            bestApo = Math.max(bestApo, Orbit.of(s).apoapsisR());

            if (cutoff) {
                return insertion(s, t, psi, stageIdx, propLeft, g, gravLoss, dragLoss, steerLoss, burned, maxQ,
                        points, recordTrajectory);
            }

            if (propLeft <= 1e-6) {
                if (stageIdx == stages.size() - 1) {
                    return fail(String.format("Propellant exhausted; apoapsis reached only %.0f km",
                            (bestApo - R_EARTH) / 1000), targetApoapsisR - bestApo, g, points);
                }
                s.m -= st.dryKg();
                stageIdx++;
                propLeft = stages.get(stageIdx).propellantKg();
            }
        }
        return fail("Time limit exceeded", targetApoapsisR - bestApo, g, points);
    }

    // ------------------------------------------------------------------ guidance

    private double pitch(Guidance g, double t, int stageIdx, State s,
                         double upperStart, double upperPsi0, double upperDuration) {
        if (stageIdx == 0) {
            if (t < Guidance.VERTICAL_RISE_S) return 90;
            if (t < Guidance.VERTICAL_RISE_S + Guidance.KICK_DURATION_S) return 90 - g.pitchKickDeg();
            return relativeFlightPath(s);
        }
        double frac = Math.min(1, (t - upperStart) / upperDuration);
        return upperPsi0 + (g.finalPitchDeg() - upperPsi0) * frac;
    }

    private double relativeFlightPath(State s) {
        double vtRel = s.vt - airSpeed(s.r);
        return Math.toDegrees(Math.atan2(s.vr, vtRel));
    }

    // ------------------------------------------------------------------ dynamics

    private double airSpeed(double r) {
        return surfaceRotationSpeed * r / R_EARTH;
    }

    private static double density(double r) {
        double h = r - R_EARTH;
        return h > 150_000 ? 0 : SEA_LEVEL_DENSITY * Math.exp(-h / SCALE_HEIGHT);
    }

    /**
     * Speed of sound from a simple two-layer standard atmosphere (troposphere lapse
     * up to 11 km, isothermal above). Good enough to place Mach number for drag
     * lookups through the dense part of the ascent; irrelevant once density ~ 0.
     */
    private static double speedOfSound(double r) {
        double h = r - R_EARTH;
        double t = h <= 11_000 ? 288.15 - 0.0065 * h : 216.65;
        return Math.sqrt(1.4 * 287.05 * t);
    }

    private double dynamicPressure(State s) {
        double vtRel = s.vt - airSpeed(s.r);
        return 0.5 * density(s.r) * (s.vr * s.vr + vtRel * vtRel);
    }

    /** Drag force components (radial, transverse) in newtons. */
    private double[] drag(State s) {
        double vtRel = s.vt - airSpeed(s.r);
        double vRel = Math.hypot(s.vr, vtRel);
        if (vRel < 1e-9) return new double[]{0, 0};
        double mach = vRel / speedOfSound(s.r);
        double d = 0.5 * density(s.r) * vRel * vRel * vehicle.dragCoefficientAt(mach) * vehicle.referenceArea();
        return new double[]{-d * s.vr / vRel, -d * vtRel / vRel};
    }

    private double[] derivatives(State s, double thrust, double psiDeg, double mdot) {
        double psi = Math.toRadians(psiDeg);
        double[] d = drag(s);
        double ar = (thrust * Math.sin(psi) + d[0]) / s.m - MU_EARTH / (s.r * s.r) + s.vt * s.vt / s.r;
        double at = (thrust * Math.cos(psi) + d[1]) / s.m - s.vr * s.vt / s.r;
        return new double[]{s.vr, s.vt / s.r, ar, at, -mdot};
    }

    private State rk4(State s, double dt, StageSpec stage, double psi) {
        double thrust = stage.thrustN();
        double mdot = stage.massFlow();
        double[] k1 = derivatives(s, thrust, psi, mdot);
        double[] k2 = derivatives(add(s, k1, dt / 2), thrust, psi, mdot);
        double[] k3 = derivatives(add(s, k2, dt / 2), thrust, psi, mdot);
        double[] k4 = derivatives(add(s, k3, dt), thrust, psi, mdot);
        State n = s.copy();
        n.r += dt / 6 * (k1[0] + 2 * k2[0] + 2 * k3[0] + k4[0]);
        n.theta += dt / 6 * (k1[1] + 2 * k2[1] + 2 * k3[1] + k4[1]);
        n.vr += dt / 6 * (k1[2] + 2 * k2[2] + 2 * k3[2] + k4[2]);
        n.vt += dt / 6 * (k1[3] + 2 * k2[3] + 2 * k3[3] + k4[3]);
        n.m += dt / 6 * (k1[4] + 2 * k2[4] + 2 * k3[4] + k4[4]);
        return n;
    }

    private static State add(State s, double[] k, double h) {
        return new State(s.r + h * k[0], s.theta + h * k[1], s.vr + h * k[2], s.vt + h * k[3], s.m + h * k[4]);
    }

    // ------------------------------------------------------------------ helpers

    private AscentResult insertion(State s, double t, double psi, int stageIdx, double propLeft, Guidance g,
                                   double gravLoss, double dragLoss, double steerLoss, double burned, double maxQ,
                                   List<TrajectoryPoint> points, boolean record) {
        if (record) points.add(sample(t, s, psi, stageIdx + 1));
        if (s.vr < 0) {
            return fail("Cutoff after apoapsis; trajectory is descending", 50_000, g, points);
        }
        Orbit o = Orbit.of(s);
        if (Double.isInfinite(o.apoapsisR())) {
            return fail("Escape trajectory at cutoff", 50_000, g, points);
        }
        double insertionDv = circularizeAt(o, targetApoapsisR);
        double avail = remainingDeltaV(s.m, stageIdx, propLeft);
        if (record) addCoast(points, s, t);
        return new AscentResult(true, null, 0, g, t, s.r - R_EARTH,
                o.apoapsisR() - R_EARTH, o.periapsisR() - R_EARTH, insertionDv, avail,
                gravLoss, dragLoss, steerLoss, burned, maxQ, points);
    }

    /**
     * Delta-v to go from the post-cutoff ellipse to a circular orbit of radius rt.
     * Burn 1 at apoapsis moves periapsis to rt; burn 2 at rt circularises.
     * When the apoapsis equals rt this reduces to a single circularisation burn.
     */
    static double circularizeAt(Orbit o, double rt) {
        double ra = o.apoapsisR();
        double vApo = o.h() / ra;
        if (Math.abs(ra - rt) < 1.0) {
            return Math.abs(Math.sqrt(MU_EARTH / rt) - vApo);
        }
        double vNewApo = Math.sqrt(2 * MU_EARTH * rt / (ra * (ra + rt)));
        double vAtRt = Math.sqrt(2 * MU_EARTH * ra / (rt * (ra + rt)));
        return Math.abs(vNewApo - vApo) + Math.abs(vAtRt - Math.sqrt(MU_EARTH / rt));
    }

    /**
     * Delta-v still available after cutoff. If cutoff happens before the last stage, the current
     * stage is jettisoned together with its unused propellant (boosters are not relit).
     */
    private double remainingDeltaV(double mass, int stageIdx, double propLeft) {
        List<StageSpec> stages = vehicle.stages();
        double dv = 0;
        if (stageIdx == stages.size() - 1) {
            dv = stages.get(stageIdx).exhaustVelocity() * Math.log(mass / (mass - propLeft));
        }
        mass -= propLeft + stages.get(stageIdx).dryKg();
        for (int k = stageIdx + 1; k < stages.size(); k++) {
            StageSpec st = stages.get(k);
            dv += st.exhaustVelocity() * Math.log(mass / (mass - st.propellantKg()));
            mass -= st.propellantKg() + st.dryKg();
        }
        return dv;
    }

    private TrajectoryPoint sample(double t, State s, double psi, int stage) {
        return new TrajectoryPoint(t, (s.r - R_EARTH) / 1000, R_EARTH * s.theta / 1000,
                Math.hypot(s.vr, s.vt), Math.toDegrees(Math.atan2(s.vr, s.vt)), psi, s.m,
                dynamicPressure(s) / 1000, stage);
    }

    /** Appends unpowered coast samples from cutoff up to apoapsis (Kepler propagation by small steps). */
    private void addCoast(List<TrajectoryPoint> pts, State s, double t0) {
        State c = s.copy();
        double t = t0;
        int guard = 0;
        while (c.vr > 0 && guard++ < 2000) {
            double[] k = {c.vr, c.vt / c.r, -MU_EARTH / (c.r * c.r) + c.vt * c.vt / c.r, -c.vr * c.vt / c.r};
            double h = 10;
            c.r += k[0] * h;
            c.theta += k[1] * h;
            c.vr += k[2] * h;
            c.vt += k[3] * h;
            t += h;
            if (guard % 6 == 0) pts.add(sample(t, c, Math.toDegrees(Math.atan2(c.vr, c.vt)), 0));
        }
    }

    private AscentResult fail(String reason, double shortfall, Guidance g, List<TrajectoryPoint> pts) {
        return new AscentResult(false, reason, Math.max(shortfall, 1), g, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, pts);
    }

    /** Two-body orbit derived from a polar state. */
    record Orbit(double h, double energy, double apoapsisR, double periapsisR) {
        static Orbit of(State s) {
            double v2 = s.vr * s.vr + s.vt * s.vt;
            double h = s.r * s.vt;
            double energy = v2 / 2 - MU_EARTH / s.r;
            if (energy >= 0) return new Orbit(h, energy, Double.POSITIVE_INFINITY, 0);
            double a = -MU_EARTH / (2 * energy);
            double e = Math.sqrt(Math.max(0, 1 + 2 * energy * h * h / (MU_EARTH * MU_EARTH)));
            return new Orbit(h, energy, a * (1 + e), a * (1 - e));
        }
    }
}
