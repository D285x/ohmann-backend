package com.anurag.ECE.physics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.anurag.ECE.physics.Constants.MU_SUN;

/** Hohmann transfer windows between planets on circular coplanar orbits. */
public final class InterplanetaryPlanner {

    private InterplanetaryPlanner() {
    }

    public record Window(Instant departure, Instant arrival, double timeOfFlightDays, double phaseAngleDeg,
                         double vInfDepartureMs, double vInfArrivalMs, double c3Km2s2,
                         double departureDvMs, double arrivalDvMs) {
        public double totalDvMs() {
            return departureDvMs + arrivalDvMs;
        }
    }

    public static double synodicPeriodDays(Body a, Body b) {
        return 360.0 / Math.abs(a.meanMotionDegPerDay() - b.meanMotionDegPerDay());
    }

    /**
     * @param parkingAltM altitude of the circular parking orbit at the origin planet
     * @param captureAltM altitude of the circular capture orbit at the destination (negative = flyby, no capture)
     */
    public static List<Window> windows(Body origin, Body dest, Instant after, int count,
                                       double parkingAltM, double captureAltM) {
        if (origin.name().equalsIgnoreCase(dest.name())) throw new IllegalArgumentException("Origin and destination must differ");
        double r1 = origin.semiMajorAxisM();
        double r2 = dest.semiMajorAxisM();
        double at = (r1 + r2) / 2;
        double tofS = Math.PI * Math.sqrt(at * at * at / MU_SUN);
        double tofDays = tofS / 86400;

        double requiredPhase = OrbitalMechanics.normalize(180 - dest.meanMotionDegPerDay() * tofDays);
        double jd0 = OrbitalMechanics.julianDate(after);
        double phaseNow = dest.meanLongitudeAt(jd0) - origin.meanLongitudeAt(jd0);
        double rel = dest.meanMotionDegPerDay() - origin.meanMotionDegPerDay();
        double synodic = synodicPeriodDays(origin, dest);
        double firstDays = ((requiredPhase - phaseNow) / rel) % synodic;
        if (firstDays < 0) firstDays += synodic;

        double vPlanet1 = Math.sqrt(MU_SUN / r1);
        double vPlanet2 = Math.sqrt(MU_SUN / r2);
        double vT1 = Math.sqrt(MU_SUN * (2 / r1 - 1 / at));
        double vT2 = Math.sqrt(MU_SUN * (2 / r2 - 1 / at));
        double vInf1 = Math.abs(vT1 - vPlanet1);
        double vInf2 = Math.abs(vPlanet2 - vT2);

        double depDv = hyperbolicBurn(origin, parkingAltM, vInf1);
        double arrDv = captureAltM < 0 ? 0 : hyperbolicBurn(dest, captureAltM, vInf2);

        List<Window> out = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            double days = firstDays + k * synodic;
            Instant dep = after.plus(Duration.ofSeconds(Math.round(days * 86400)));
            Instant arr = dep.plus(Duration.ofSeconds(Math.round(tofS)));
            out.add(new Window(dep, arr, tofDays, requiredPhase, vInf1, vInf2,
                    vInf1 * vInf1 / 1e6, depDv, arrDv));
        }
        return out;
    }

    /** Burn from (or to) a circular orbit at altitude alt to reach hyperbolic excess speed vInf. */
    public static double hyperbolicBurn(Body p, double altM, double vInf) {
        double rp = p.radiusM() + altM;
        return Math.sqrt(vInf * vInf + 2 * p.mu() / rp) - Math.sqrt(p.mu() / rp);
    }
}
