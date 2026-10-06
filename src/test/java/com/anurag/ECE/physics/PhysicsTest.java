package com.anurag.ECE.physics;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsTest {

    static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    static final Body EARTH = new Body("Earth", Constants.AU, 100.4645, Constants.MU_EARTH, 6_378_137);
    static final Body MARS = new Body("Mars", 1.52368 * Constants.AU, 355.4533, 4.282837e13, 3_389_500);

    static VehicleSpec falcon9() {
        return new VehicleSpec("Falcon 9", List.of(
                new StageSpec("S1", 395_700, 25_600, 7_607_000, 300),
                new StageSpec("S2", 92_670, 3_900, 981_000, 348)), 3.7, 0.3);
    }

    @AfterAll
    static void shutdown() {
        POOL.shutdown();
    }

    @Test
    void ssoInclinationMatchesReference() {
        // J2 model gives about 98.2 deg at 700 km
        assertEquals(98.19, OrbitalMechanics.ssoInclination(700_000), 0.2);
    }

    @Test
    void hohmannLeoToGeo() {
        double r1 = Constants.R_EARTH + 300_000, r2 = Constants.R_EARTH + Constants.GEO_ALTITUDE;
        double[] dv = OrbitalMechanics.hohmann(Constants.MU_EARTH, r1, r2);
        assertEquals(3900, dv[0] + dv[1], 60);
    }

    @Test
    void azimuthDueEastFromCape() {
        var az = OrbitalMechanics.launchAzimuth(28.5, 28.5, 7800, 35, 120);
        assertEquals(90, az.azimuthDeg(), 0.5);
        assertTrue(az.inPlane());
    }

    @Test
    void keplerPeriods() {
        assertEquals(365.26, EARTH.periodDays(), 0.1);
        assertEquals(686.98, MARS.periodDays(), 1.0);
    }

    @Test
    void earthToMarsHohmann() {
        var w = InterplanetaryPlanner.windows(EARTH, MARS, Instant.parse("2026-01-01T00:00:00Z"), 2, 200_000, 400_000);
        assertEquals(259, w.get(0).timeOfFlightDays(), 2);
        assertEquals(780, InterplanetaryPlanner.synodicPeriodDays(EARTH, MARS), 2);
        assertEquals(2945, w.get(0).vInfDepartureMs(), 30);
    }

    @Test
    void dragCurveInterpolatesAndClamps() {
        VehicleSpec withCurve = new VehicleSpec("Test", falcon9().stages(), 3.7, 0.3,
                new double[]{0.5, 1.0, 1.5, 2.0}, new double[]{0.2, 0.9, 0.6, 0.4});
        assertEquals(0.2, withCurve.dragCoefficientAt(0.1), 1e-9, "below the table clamps to the first point");
        assertEquals(0.9, withCurve.dragCoefficientAt(1.0), 1e-9, "exact table point");
        assertEquals(0.75, withCurve.dragCoefficientAt(1.25), 1e-9, "midpoint interpolates linearly");
        assertEquals(0.4, withCurve.dragCoefficientAt(3.0), 1e-9, "above the table clamps to the last point");
        assertEquals(0.3, falcon9().dragCoefficientAt(1.0), 1e-9, "no curve: constant Cd regardless of Mach");
    }

    @Test
    void falcon9ReachesLeo() {
        var opt = new AscentOptimizer(POOL);
        var az = OrbitalMechanics.launchAzimuth(28.56, 28.56, 7700, 35, 120);
        AscentResult r = opt.optimize(falcon9(), 10_000, 200_000, az.rotationAssistMs(), 0);
        assertTrue(r.success());
        var cap = opt.payloadCapacity(falcon9(), 200_000, az.rotationAssistMs(), 0, r.guidance());
        assertTrue(cap.payloadKg() > 12_000 && cap.payloadKg() < 30_000);
    }
}
