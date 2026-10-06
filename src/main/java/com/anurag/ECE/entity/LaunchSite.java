package com.anurag.ECE.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "launch_sites")
public class LaunchSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 60)
    private String country;

    /** Geodetic latitude, degrees (north positive). */
    private double latitudeDeg;

    /** Longitude, degrees (east positive). */
    private double longitudeDeg;

    /** Range-safety azimuth corridor, degrees clockwise from north. */
    private double minAzimuthDeg;
    private double maxAzimuthDeg;

    public LaunchSite() {
    }

    public LaunchSite(String name, String country, double latitudeDeg, double longitudeDeg,
                      double minAzimuthDeg, double maxAzimuthDeg) {
        this.name = name;
        this.country = country;
        this.latitudeDeg = latitudeDeg;
        this.longitudeDeg = longitudeDeg;
        this.minAzimuthDeg = minAzimuthDeg;
        this.maxAzimuthDeg = maxAzimuthDeg;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public double getLatitudeDeg() { return latitudeDeg; }
    public void setLatitudeDeg(double v) { this.latitudeDeg = v; }
    public double getLongitudeDeg() { return longitudeDeg; }
    public void setLongitudeDeg(double v) { this.longitudeDeg = v; }
    public double getMinAzimuthDeg() { return minAzimuthDeg; }
    public void setMinAzimuthDeg(double v) { this.minAzimuthDeg = v; }
    public double getMaxAzimuthDeg() { return maxAzimuthDeg; }
    public void setMaxAzimuthDeg(double v) { this.maxAzimuthDeg = v; }
}
