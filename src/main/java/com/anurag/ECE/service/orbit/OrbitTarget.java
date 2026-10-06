package com.anurag.ECE.service.orbit;

import com.anurag.ECE.entity.OrbitType;

import java.util.List;
import java.util.Locale;

/**
 * A target orbit requested by the user. Each orbit family decides its own inclination
 * and any delta-v needed after the launch vehicle reaches the insertion orbit.
 */
public abstract class OrbitTarget {

    private final double altitudeKm;

    protected OrbitTarget(double altitudeKm) {
        this.altitudeKm = altitudeKm;
    }

    public static OrbitTarget of(OrbitType type, double altitudeKm) {
        return switch (type) {
            case LEO -> new LeoTarget(altitudeKm);
            case SSO -> new SsoTarget(altitudeKm);
            case GTO -> new GtoTarget(altitudeKm);
        };
    }

    /** Altitude of the orbit the launch vehicle inserts into (km). */
    public double insertionAltitudeKm() {
        return altitudeKm;
    }

    public abstract OrbitType type();

    /** Inclination to fly, given the site latitude and the user's (optional) request. */
    public abstract double inclinationDeg(double siteLatitudeDeg, Double requestedDeg, List<String> notes);

    /** Delta-v the vehicle must still provide after insertion (m/s). Default: none. */
    public double postInsertionDv(double inclinationDeg, double siteLatitudeDeg, List<String> notes) {
        return 0;
    }

    protected static String fmt(String f, Object... args) {
        return String.format(Locale.ROOT, f, args);
    }
}
