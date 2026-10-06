package com.anurag.ECE.physics;

import java.time.Duration;
import java.time.Instant;

import static com.anurag.ECE.physics.Constants.*;

/** Closed-form orbital mechanics helpers (all angles in degrees unless stated). */
public final class OrbitalMechanics {

    private OrbitalMechanics() {
    }

    public static double circularVelocity(double mu, double r) {
        return Math.sqrt(mu / r);
    }

    /** Inclination for a circular sun-synchronous orbit at the given altitude (J2 model). */
    public static double ssoInclination(double altitudeM) {
        double a = R_EARTH + altitudeM;
        double cosI = -2 * SSO_PRECESSION_RATE * Math.pow(a, 3.5)
                / (3 * J2_EARTH * R_EARTH * R_EARTH * Math.sqrt(MU_EARTH));
        if (cosI < -1) throw new IllegalArgumentException("No sun-synchronous orbit exists at this altitude");
        return Math.toDegrees(Math.acos(cosI));
    }

    /** Hohmann transfer burns between two circular coplanar orbits. Returns {dv1, dv2}. */
    public static double[] hohmann(double mu, double r1, double r2) {
        double at = (r1 + r2) / 2;
        double dv1 = Math.abs(Math.sqrt(mu * (2 / r1 - 1 / at)) - Math.sqrt(mu / r1));
        double dv2 = Math.abs(Math.sqrt(mu / r2) - Math.sqrt(mu * (2 / r2 - 1 / at)));
        return new double[]{dv1, dv2};
    }

    /** Delta-v for a pure plane change of dInc degrees at speed v. */
    public static double planeChange(double v, double dIncDeg) {
        return 2 * v * Math.sin(Math.toRadians(dIncDeg) / 2);
    }

    /**
     * Result of the launch azimuth computation.
     *
     * @param azimuthDeg        launch azimuth in the rotating (Earth-fixed) frame, 0..360
     * @param ascending         true if the pad passes through the ascending half of the orbit
     * @param inPlane           false when inclination is below the site latitude (direct insertion impossible)
     * @param rotationAssistMs  in-plane component of the pad's rotation speed
     * @param inCorridor        whether the azimuth lies inside the site's range-safety corridor
     */
    public record Azimuth(double azimuthDeg, boolean ascending, boolean inPlane,
                          double rotationAssistMs, boolean inCorridor) {
    }

    public static Azimuth launchAzimuth(double latDeg, double incDeg, double orbitalSpeed,
                                        double corridorMin, double corridorMax) {
        double phi = Math.toRadians(latDeg);
        boolean inPlane = incDeg >= Math.abs(latDeg) && incDeg <= 180 - Math.abs(latDeg);
        double sinB = inPlane ? Math.cos(Math.toRadians(incDeg)) / Math.cos(phi) : 1.0;
        sinB = Math.max(-1, Math.min(1, sinB));
        double betaI = Math.asin(sinB);                    // inertial azimuth, northbound branch
        double vEq = OMEGA_EARTH * R_EARTH;

        double best = Double.NaN;
        boolean bestAsc = true;
        boolean found = false;
        for (boolean northbound : new boolean[]{true, false}) {
            double b = northbound ? betaI : Math.PI - betaI;
            double east = orbitalSpeed * Math.sin(b) - vEq * Math.cos(phi);
            double north = orbitalSpeed * Math.cos(b);
            double az = normalize(Math.toDegrees(Math.atan2(east, north)));
            // A northbound pass is always on the ascending half of the orbit.
            boolean ascending = northbound;
            if (Double.isNaN(best)) {
                best = az;
                bestAsc = ascending;
            }
            if (inCorridor(az, corridorMin, corridorMax)) {
                best = az;
                bestAsc = ascending;
                found = true;
                break;
            }
            if (!inPlane) break;                           // due-east launch has only one branch
        }
        double assist = vEq * Math.cos(phi) * Math.sin(Math.toRadians(best));
        return new Azimuth(best, bestAsc, inPlane, assist, found);
    }

    static boolean inCorridor(double az, double min, double max) {
        double a = normalize(az), lo = normalize(min), hi = normalize(max);
        return lo <= hi ? (a >= lo && a <= hi) : (a >= lo || a <= hi);
    }

    public static double normalize(double deg) {
        double d = deg % 360;
        return d < 0 ? d + 360 : d;
    }

    // ------------------------------------------------------------------ time

    public static double julianDate(Instant t) {
        return t.toEpochMilli() / 86_400_000.0 + 2_440_587.5;
    }

    public static double gmstDeg(Instant t) {
        return normalize(280.46061837 + SIDEREAL_DEG_PER_DAY * (julianDate(t) - J2000_JD));
    }

    /**
     * Next in-plane launch opportunity for an orbit with the given RAAN.
     * The pad must be rotated under the orbit plane: LST = RAAN + delta-longitude of the pad from the node.
     *
     * @return null when no in-plane opportunity exists (inclination below latitude)
     */
    public static Instant nextLaunchWindow(Instant after, double latDeg, double lonDeg,
                                           double incDeg, double raanDeg, boolean ascending) {
        double sinU = Math.sin(Math.toRadians(latDeg)) / Math.sin(Math.toRadians(incDeg));
        if (Math.abs(sinU) > 1) return null;
        double u = Math.asin(sinU);
        if (!ascending) u = Math.PI - u;
        double dLon = Math.toDegrees(Math.atan2(Math.cos(Math.toRadians(incDeg)) * Math.sin(u), Math.cos(u)));
        double targetLst = normalize(raanDeg + dLon);
        double lst = normalize(gmstDeg(after) + lonDeg);
        double waitDeg = normalize(targetLst - lst);
        long waitMs = Math.round(waitDeg / SIDEREAL_DEG_PER_DAY * 86_400_000.0);
        return after.plus(Duration.ofMillis(waitMs));
    }
}
