package com.anurag.ECE.service.orbit;

import com.anurag.ECE.entity.OrbitType;
import com.anurag.ECE.physics.OrbitalMechanics;

import java.util.List;

import static com.anurag.ECE.physics.Constants.*;

/**
 * Geostationary transfer orbit. The vehicle first reaches a circular parking orbit
 * (the requested altitude), then its upper stage fires the Hohmann perigee burn.
 */
public class GtoTarget extends LeoTarget {

    public GtoTarget(double parkingAltitudeKm) {
        super(parkingAltitudeKm);
    }

    @Override
    public OrbitType type() {
        return OrbitType.GTO;
    }

    @Override
    public double postInsertionDv(double inclinationDeg, double siteLatitudeDeg, List<String> notes) {
        double rPark = R_EARTH + insertionAltitudeKm() * 1000;
        double rGeo = R_EARTH + GEO_ALTITUDE;
        double[] burns = OrbitalMechanics.hohmann(MU_EARTH, rPark, rGeo);

        double finalInc = Math.max(inclinationDeg, Math.abs(siteLatitudeDeg));
        double vApo = Math.sqrt(MU_EARTH / rGeo) - burns[1];
        double vGeo = Math.sqrt(MU_EARTH / rGeo);
        double geoDv = Math.sqrt(vApo * vApo + vGeo * vGeo
                - 2 * vApo * vGeo * Math.cos(Math.toRadians(finalInc)));

        notes.add(fmt("GTO perigee burn from %.0f km parking orbit: %.0f m/s (included)",
                insertionAltitudeKm(), burns[0]));
        notes.add(fmt("Satellite apogee burn to GEO incl. %.1f deg plane change: %.0f m/s (not included)",
                finalInc, geoDv));
        return burns[0];
    }
}
