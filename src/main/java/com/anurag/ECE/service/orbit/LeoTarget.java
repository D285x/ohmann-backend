package com.anurag.ECE.service.orbit;

import com.anurag.ECE.entity.OrbitType;

import java.util.List;

/** Circular low Earth orbit; defaults to a due-east launch (inclination = site latitude). */
public class LeoTarget extends OrbitTarget {

    public LeoTarget(double altitudeKm) {
        super(altitudeKm);
    }

    @Override
    public OrbitType type() {
        return OrbitType.LEO;
    }

    @Override
    public double inclinationDeg(double siteLatitudeDeg, Double requestedDeg, List<String> notes) {
        return requestedDeg != null ? requestedDeg : Math.abs(siteLatitudeDeg);
    }
}
