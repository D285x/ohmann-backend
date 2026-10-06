package com.anurag.ECE.entity;

import com.anurag.ECE.physics.Body;
import jakarta.persistence.*;

/** A Sun-orbiting body that can be used as a transfer origin or destination. */
@Entity
@Table(name = "celestial_bodies")
public class CelestialBody {

    private static final double AU_M = 1.495978707e11;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BodyType bodyType;

    private double semiMajorAxisAu;
    private double meanLongitudeJ2000Deg;

    /** Gravitational parameter GM in km^3/s^2. */
    private double gmKm3s2;

    private double radiusKm;

    public CelestialBody() {
    }

    public CelestialBody(String name, BodyType type, double aAu, double meanLongitude, double gm, double radiusKm) {
        this.name = name;
        this.bodyType = type;
        this.semiMajorAxisAu = aAu;
        this.meanLongitudeJ2000Deg = meanLongitude;
        this.gmKm3s2 = gm;
        this.radiusKm = radiusKm;
    }

    /** Converts to SI units for the physics engine. */
    public Body toBody() {
        return new Body(name, semiMajorAxisAu * AU_M, meanLongitudeJ2000Deg, gmKm3s2 * 1e9, radiusKm * 1000);
    }

    public boolean isEarth() {
        return "earth".equalsIgnoreCase(name);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BodyType getBodyType() { return bodyType; }
    public void setBodyType(BodyType bodyType) { this.bodyType = bodyType; }
    public double getSemiMajorAxisAu() { return semiMajorAxisAu; }
    public void setSemiMajorAxisAu(double v) { this.semiMajorAxisAu = v; }
    public double getMeanLongitudeJ2000Deg() { return meanLongitudeJ2000Deg; }
    public void setMeanLongitudeJ2000Deg(double v) { this.meanLongitudeJ2000Deg = v; }
    public double getGmKm3s2() { return gmKm3s2; }
    public void setGmKm3s2(double v) { this.gmKm3s2 = v; }
    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double v) { this.radiusKm = v; }
}
