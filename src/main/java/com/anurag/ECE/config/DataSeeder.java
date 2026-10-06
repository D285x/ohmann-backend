package com.anurag.ECE.config;

import com.anurag.ECE.entity.AppUser;
import com.anurag.ECE.entity.BodyType;
import com.anurag.ECE.entity.CelestialBody;
import com.anurag.ECE.entity.LaunchSite;
import com.anurag.ECE.entity.LaunchVehicle;
import com.anurag.ECE.entity.Stage;
import com.anurag.ECE.entity.UserRole;
import com.anurag.ECE.repository.AppUserRepository;
import com.anurag.ECE.repository.CelestialBodyRepository;
import com.anurag.ECE.repository.LaunchSiteRepository;
import com.anurag.ECE.repository.LaunchVehicleRepository;
import com.anurag.ECE.util.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads a starter catalogue on first run. Stage figures are rounded public estimates;
 * strap-on boosters are folded into stage 1 because the simulator models sequential staging only.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final LaunchVehicleRepository vehicles;
    private final LaunchSiteRepository sites;
    private final CelestialBodyRepository bodies;
    private final AppUserRepository users;

    /** Shared demo login, recreated on every start so reviewers never have to register first. */
    public static final String DEMO_EMAIL = "demo@ohmann.app";
    public static final String DEMO_PASSWORD = "ohmann-demo";

    public DataSeeder(LaunchVehicleRepository vehicles, LaunchSiteRepository sites, CelestialBodyRepository bodies,
                      AppUserRepository users) {
        this.vehicles = vehicles;
        this.sites = sites;
        this.bodies = bodies;
        this.users = users;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (vehicles.count() == 0) {
            vehicles.saveAll(List.of(
                    vehicle("Falcon 9 Block 5 (expendable)", "SpaceX", "USA", 3.7, 0.30,
                            new Stage("Stage 1 (9x Merlin 1D)", 395_700, 25_600, 7_607, 300),
                            new Stage("Stage 2 (Merlin Vacuum)", 92_670, 3_900, 981, 348)),
                    vehicle("PSLV-XL", "ISRO", "India", 2.8, 0.30,
                            new Stage("PS1 + 6 PSOM-XL", 210_000, 45_000, 5_000, 260),
                            new Stage("PS2 (Vikas)", 42_000, 5_300, 799, 293),
                            new Stage("PS3 (HPS3)", 7_650, 1_100, 240, 295),
                            new Stage("PS4 (2x PS4 engines)", 2_500, 920, 14.6, 308)),
                    vehicle("LVM3", "ISRO", "India", 4.0, 0.30,
                            new Stage("2x S200 boosters", 410_000, 62_000, 10_000, 250),
                            new Stage("L110 core (2x Vikas)", 115_000, 12_000, 1_600, 285),
                            new Stage("C25 cryogenic (CE-20)", 28_000, 5_000, 200, 442)),
                    vehicle("Electron", "Rocket Lab", "USA / New Zealand", 1.2, 0.30,
                            new Stage("Stage 1 (9x Rutherford)", 9_250, 950, 224, 303),
                            new Stage("Stage 2 (Rutherford Vacuum)", 2_050, 250, 25.8, 343))));
            log.info("Seeded {} launch vehicles", vehicles.count());
        }
        if (sites.count() == 0) {
            sites.saveAll(List.of(
                    new LaunchSite("Satish Dhawan Space Centre, Sriharikota", "India", 13.72, 80.23, 85, 200),
                    new LaunchSite("Cape Canaveral SLC-40", "USA", 28.56, -80.58, 35, 120),
                    new LaunchSite("Vandenberg SLC-4E", "USA", 34.63, -120.61, 140, 210),
                    new LaunchSite("Guiana Space Centre, Kourou", "France", 5.24, -52.77, 350, 95),
                    new LaunchSite("Baikonur Cosmodrome", "Kazakhstan", 45.96, 63.31, 25, 65),
                    new LaunchSite("Rocket Lab LC-1, Mahia", "New Zealand", -39.26, 177.86, 35, 200)));
            log.info("Seeded {} launch sites", sites.count());
        }
        if (bodies.count() == 0) {
            // orbit radius (AU), J2000 mean longitude (deg), GM (km^3/s^2), mean radius (km)
            bodies.saveAll(List.of(
                    new CelestialBody("Mercury", BodyType.PLANET, 0.38710, 252.2503, 22_032, 2_439.7),
                    new CelestialBody("Venus", BodyType.PLANET, 0.72333, 181.9791, 324_859, 6_051.8),
                    new CelestialBody("Earth", BodyType.PLANET, 1.00000, 100.4645, 398_600.4418, 6_378.137),
                    new CelestialBody("Mars", BodyType.PLANET, 1.52368, 355.4533, 42_828.37, 3_389.5),
                    new CelestialBody("Jupiter", BodyType.PLANET, 5.20260, 34.3515, 126_686_534, 69_911),
                    new CelestialBody("Saturn", BodyType.PLANET, 9.55491, 50.0775, 37_931_187, 58_232),
                    new CelestialBody("Uranus", BodyType.PLANET, 19.21845, 314.0550, 5_793_939, 25_362),
                    new CelestialBody("Neptune", BodyType.PLANET, 30.11039, 304.3487, 6_836_529, 24_622)));
            log.info("Seeded {} celestial bodies", bodies.count());
        }
        if (!users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            AppUser demo = new AppUser();
            demo.setFullName("Demo Operator");
            demo.setEmail(DEMO_EMAIL);
            demo.setOrganization("OhMann demo");
            demo.setRole(UserRole.MISSION_PLANNER);
            String salt = PasswordHasher.newSalt();
            demo.setPasswordSalt(salt);
            demo.setPasswordHash(PasswordHasher.hash(DEMO_PASSWORD, salt));
            users.save(demo);
            log.info("Seeded demo operator {}", DEMO_EMAIL);
        }
    }

    private static LaunchVehicle vehicle(String name, String maker, String country, double dia, double cd,
                                         Stage... stages) {
        LaunchVehicle v = new LaunchVehicle(name, maker, country, dia, cd);
        for (Stage s : stages) v.addStage(s);
        return v;
    }
}
