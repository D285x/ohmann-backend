package com.anurag.ECE.physics;

import static com.anurag.ECE.physics.Constants.*;

/**
 * A body on a circular heliocentric orbit, as used by the transfer planner.
 * The orbital period follows from Kepler's third law, so only the orbit size is needed.
 *
 * @param semiMajorAxisM        orbit radius (m)
 * @param meanLongitudeJ2000Deg mean longitude at the J2000 epoch (deg)
 * @param mu                    gravitational parameter GM (m^3/s^2)
 * @param radiusM               mean radius (m)
 */
public record Body(String name, double semiMajorAxisM, double meanLongitudeJ2000Deg, double mu, double radiusM) {

    public Body {
        if (semiMajorAxisM <= 0 || mu <= 0 || radiusM <= 0) {
            throw new IllegalArgumentException("Orbit radius, GM and radius must be positive for " + name);
        }
    }

    public double periodDays() {
        return 2 * Math.PI * Math.sqrt(Math.pow(semiMajorAxisM, 3) / MU_SUN) / 86_400.0;
    }

    public double meanMotionDegPerDay() {
        return 360.0 / periodDays();
    }

    public double meanLongitudeAt(double julianDate) {
        return OrbitalMechanics.normalize(meanLongitudeJ2000Deg + meanMotionDegPerDay() * (julianDate - J2000_JD));
    }

    public double surfaceGravity() {
        return mu / (radiusM * radiusM);
    }

    public double circularSpeed() {
        return Math.sqrt(MU_SUN / semiMajorAxisM);
    }
}
