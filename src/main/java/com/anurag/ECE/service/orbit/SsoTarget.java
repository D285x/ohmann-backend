package com.anurag.ECE.service.orbit;

import com.anurag.ECE.entity.OrbitType;
import com.anurag.ECE.exception.InvalidRequestException;
import com.anurag.ECE.physics.OrbitalMechanics;

import java.util.List;

/** Sun-synchronous orbit: the inclination is fixed by the altitude (J2 precession). */
public class SsoTarget extends OrbitTarget {

    public SsoTarget(double altitudeKm) {
        super(altitudeKm);
    }

    @Override
    public OrbitType type() {
        return OrbitType.SSO;
    }

    @Override
    public double inclinationDeg(double siteLatitudeDeg, Double requestedDeg, List<String> notes) {
        double inc;
        try {
            inc = OrbitalMechanics.ssoInclination(insertionAltitudeKm() * 1000);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        notes.add(fmt("Sun-synchronous inclination at %.0f km: %.2f deg", insertionAltitudeKm(), inc));
        if (requestedDeg != null && Math.abs(requestedDeg - inc) > 0.01) {
            notes.add("Requested inclination ignored for SSO (fixed by altitude)");
        }
        return inc;
    }
}
