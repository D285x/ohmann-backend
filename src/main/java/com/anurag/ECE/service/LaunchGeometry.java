package com.anurag.ECE.service;

import com.anurag.ECE.entity.LaunchSite;
import com.anurag.ECE.entity.OrbitType;
import com.anurag.ECE.physics.OrbitalMechanics;
import com.anurag.ECE.service.orbit.OrbitTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.anurag.ECE.physics.Constants.*;

/**
 * Mission geometry derived from site and target orbit: inclination, launch azimuth,
 * Earth-rotation assist and the delta-v needed after insertion (plane change, GTO burn).
 */
public record LaunchGeometry(double insertionAltM, double inclinationDeg, OrbitalMechanics.Azimuth azimuth,
                             double postInsertionDv, List<String> notes) {

    public static LaunchGeometry of(LaunchSite site, OrbitType type, double altitudeKm, Double requestedInc) {
        return of(site, OrbitTarget.of(type, altitudeKm), requestedInc);
    }

    public static LaunchGeometry of(LaunchSite site, OrbitTarget target, Double requestedInc) {
        List<String> notes = new ArrayList<>();
        double lat = site.getLatitudeDeg();
        double inc = target.inclinationDeg(lat, requestedInc, notes);

        double altM = target.insertionAltitudeKm() * 1000;
        double vCirc = OrbitalMechanics.circularVelocity(MU_EARTH, R_EARTH + altM);
        OrbitalMechanics.Azimuth az = OrbitalMechanics.launchAzimuth(lat, inc, vCirc,
                site.getMinAzimuthDeg(), site.getMaxAzimuthDeg());

        double extra = 0;
        if (!az.inPlane()) {
            double reachable = inc < Math.abs(lat) ? Math.abs(lat) : 180 - Math.abs(lat);
            double dInc = Math.abs(inc - reachable);
            double dv = OrbitalMechanics.planeChange(vCirc, dInc);
            extra += dv;
            notes.add(fmt("Inclination %.2f deg is not directly reachable from latitude %.2f deg: "
                    + "launch due east and change plane by %.2f deg in orbit (+%.0f m/s)", inc, lat, dInc, dv));
        }
        if (!az.inCorridor()) {
            notes.add(fmt("Azimuth %.1f deg is outside the site's range-safety corridor (%.0f-%.0f deg); "
                            + "a dogleg manoeuvre or different site would be required",
                    az.azimuthDeg(), site.getMinAzimuthDeg(), site.getMaxAzimuthDeg()));
        }
        if (az.rotationAssistMs() < 0) {
            notes.add(fmt("Retrograde launch: Earth rotation costs %.0f m/s", -az.rotationAssistMs()));
        } else {
            notes.add(fmt("Earth rotation assist: %.0f m/s", az.rotationAssistMs()));
        }

        extra += target.postInsertionDv(inc, lat, notes);
        return new LaunchGeometry(altM, inc, az, extra, notes);
    }

    private static String fmt(String f, Object... args) {
        return String.format(Locale.ROOT, f, args);
    }
}
