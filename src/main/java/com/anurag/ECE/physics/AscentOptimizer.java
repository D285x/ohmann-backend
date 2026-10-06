package com.anurag.ECE.physics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Finds the pitch program that maximises the delta-v margin left after orbit insertion.
 * <p>
 * A coarse grid over (pitch kick, final pitch) is evaluated in parallel on a thread pool,
 * followed by successive local grid refinements around the best point. The payload capacity
 * is found by bisection on payload mass, re-optimising the pitch program at every step.
 */
public class AscentOptimizer {

    public static final double KICK_MIN = 0.2, KICK_MAX = 30.0;
    public static final double FINAL_MIN = -30.0, FINAL_MAX = 40.0;

    private final ExecutorService pool;

    public AscentOptimizer(ExecutorService pool) {
        this.pool = pool;
    }

    /** Scalar objective: lower is better. Infeasible trajectories are ranked by how close they got. */
    static double cost(AscentResult r, double extraDv) {
        if (!r.success()) return 1e7 + r.apoapsisShortfallM();
        double margin = r.margin(extraDv);
        return -margin;
    }

    public AscentResult optimize(VehicleSpec v, double payloadKg, double targetAltM,
                                 double rotationAssist, double extraDv) {
        AscentSimulator sim = new AscentSimulator(v, payloadKg, targetAltM, rotationAssist);
        List<Guidance> grid = grid(KICK_MIN, KICK_MAX, 14, FINAL_MIN, FINAL_MAX, 13);
        AscentResult best = evaluate(sim, grid, extraDv);
        return refine(sim, best, extraDv, (KICK_MAX - KICK_MIN) / 13, (FINAL_MAX - FINAL_MIN) / 12, 4);
    }

    /** Local optimisation only, seeded by a known good guidance (used inside the payload bisection). */
    public AscentResult optimizeNear(VehicleSpec v, double payloadKg, double targetAltM,
                                     double rotationAssist, double extraDv, Guidance seed) {
        AscentSimulator sim = new AscentSimulator(v, payloadKg, targetAltM, rotationAssist);
        AscentResult start = sim.simulate(seed, false);
        return refine(sim, start, extraDv, 1.5, 8, 3);
    }

    private AscentResult refine(AscentSimulator sim, AscentResult best, double extraDv,
                                double spanKick, double spanFinal, int levels) {
        for (int level = 0; level < levels; level++) {
            Guidance c = best.guidance();
            List<Guidance> local = grid(
                    Math.max(KICK_MIN, c.pitchKickDeg() - spanKick), Math.min(KICK_MAX, c.pitchKickDeg() + spanKick), 5,
                    Math.max(FINAL_MIN, c.finalPitchDeg() - spanFinal), Math.min(FINAL_MAX, c.finalPitchDeg() + spanFinal), 5);
            local.add(c);
            AscentResult cand = evaluate(sim, local, extraDv);
            if (cost(cand, extraDv) <= cost(best, extraDv)) best = cand;
            spanKick /= 2;
            spanFinal /= 2;
        }
        return best;
    }

    /**
     * Largest payload for which the margin stays non-negative.
     *
     * @return {payloadKg, guidance of the final feasible solution} or null if even zero payload fails
     */
    public CapacityResult payloadCapacity(VehicleSpec v, double targetAltM, double rotationAssist,
                                          double extraDv, Guidance seed) {
        AscentResult zero = optimizeNear(v, 0, targetAltM, rotationAssist, extraDv, seed);
        if (!zero.success() || zero.margin(extraDv) < 0) {
            zero = optimize(v, 0, targetAltM, rotationAssist, extraDv);
            if (!zero.success() || zero.margin(extraDv) < 0) return new CapacityResult(0, zero.guidance());
        }
        double lo = 0, hi = v.liftoffMass(0) * 0.08;
        Guidance g = zero.guidance();
        // make sure hi is infeasible
        while (feasible(optimizeNear(v, hi, targetAltM, rotationAssist, extraDv, g), extraDv)) {
            lo = hi;
            hi *= 1.5;
        }
        while (hi - lo > 10) {
            double mid = (lo + hi) / 2;
            AscentResult r = optimizeNear(v, mid, targetAltM, rotationAssist, extraDv, g);
            if (feasible(r, extraDv)) {
                lo = mid;
                g = r.guidance();
            } else {
                hi = mid;
            }
        }
        return new CapacityResult(Math.floor(lo), g);
    }

    public record CapacityResult(double payloadKg, Guidance guidance) {
    }

    private static boolean feasible(AscentResult r, double extraDv) {
        return r.success() && r.margin(extraDv) >= 0;
    }

    private AscentResult evaluate(AscentSimulator sim, List<Guidance> candidates, double extraDv) {
        List<Callable<AscentResult>> tasks = new ArrayList<>();
        for (Guidance g : candidates) tasks.add(() -> sim.simulate(g, false));
        try {
            List<AscentResult> results = new ArrayList<>();
            for (Future<AscentResult> f : pool.invokeAll(tasks)) results.add(f.get());
            return results.stream().min(Comparator.comparingDouble(r -> cost(r, extraDv))).orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Optimization interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Simulation failed: " + e.getCause().getMessage(), e.getCause());
        }
    }

    private static List<Guidance> grid(double k0, double k1, int nk, double f0, double f1, int nf) {
        List<Guidance> list = new ArrayList<>();
        for (int i = 0; i < nk; i++) {
            double k = nk == 1 ? k0 : k0 + (k1 - k0) * i / (nk - 1);
            for (int j = 0; j < nf; j++) {
                double f = nf == 1 ? f0 : f0 + (f1 - f0) * j / (nf - 1);
                list.add(new Guidance(k, f));
            }
        }
        return list;
    }
}
