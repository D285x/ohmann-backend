package com.anurag.ECE.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** A saved launch-to-orbit plan together with its optimization results. */
@Entity
@Table(name = "mission_plans")
public class MissionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String missionName;

    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id")
    private LaunchVehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "site_id")
    private LaunchSite site;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "planned_by_id")
    private AppUser plannedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private OrbitType orbitType;

    private double targetAltitudeKm;
    private double inclinationDeg;
    private double payloadKg;

    // ---- results ----
    private boolean feasible;
    private double launchAzimuthDeg;
    private double pitchKickDeg;
    private double finalPitchDeg;
    private double gravityLossMs;
    private double dragLossMs;
    private double steeringLossMs;
    private double ascentDvMs;
    private double postInsertionDvMs;
    private double totalDvMs;
    private double dvMarginMs;
    private double maxPayloadKg;
    private double maxQkPa;
    private double insertionTimeS;
    private Instant nextWindowUtc;

    @Column(length = 2000)
    private String notes;

    public Long getId() { return id; }
    public String getMissionName() { return missionName; }
    public void setMissionName(String missionName) { this.missionName = missionName; }
    public Instant getCreatedAt() { return createdAt; }
    public LaunchVehicle getVehicle() { return vehicle; }
    public void setVehicle(LaunchVehicle vehicle) { this.vehicle = vehicle; }
    public LaunchSite getSite() { return site; }
    public void setSite(LaunchSite site) { this.site = site; }
    public AppUser getPlannedBy() { return plannedBy; }
    public void setPlannedBy(AppUser plannedBy) { this.plannedBy = plannedBy; }
    public OrbitType getOrbitType() { return orbitType; }
    public void setOrbitType(OrbitType orbitType) { this.orbitType = orbitType; }
    public double getTargetAltitudeKm() { return targetAltitudeKm; }
    public void setTargetAltitudeKm(double v) { this.targetAltitudeKm = v; }
    public double getInclinationDeg() { return inclinationDeg; }
    public void setInclinationDeg(double v) { this.inclinationDeg = v; }
    public double getPayloadKg() { return payloadKg; }
    public void setPayloadKg(double v) { this.payloadKg = v; }
    public boolean isFeasible() { return feasible; }
    public void setFeasible(boolean feasible) { this.feasible = feasible; }
    public double getLaunchAzimuthDeg() { return launchAzimuthDeg; }
    public void setLaunchAzimuthDeg(double v) { this.launchAzimuthDeg = v; }
    public double getPitchKickDeg() { return pitchKickDeg; }
    public void setPitchKickDeg(double v) { this.pitchKickDeg = v; }
    public double getFinalPitchDeg() { return finalPitchDeg; }
    public void setFinalPitchDeg(double v) { this.finalPitchDeg = v; }
    public double getGravityLossMs() { return gravityLossMs; }
    public void setGravityLossMs(double v) { this.gravityLossMs = v; }
    public double getDragLossMs() { return dragLossMs; }
    public void setDragLossMs(double v) { this.dragLossMs = v; }
    public double getSteeringLossMs() { return steeringLossMs; }
    public void setSteeringLossMs(double v) { this.steeringLossMs = v; }
    public double getAscentDvMs() { return ascentDvMs; }
    public void setAscentDvMs(double v) { this.ascentDvMs = v; }
    public double getPostInsertionDvMs() { return postInsertionDvMs; }
    public void setPostInsertionDvMs(double v) { this.postInsertionDvMs = v; }
    public double getTotalDvMs() { return totalDvMs; }
    public void setTotalDvMs(double v) { this.totalDvMs = v; }
    public double getDvMarginMs() { return dvMarginMs; }
    public void setDvMarginMs(double v) { this.dvMarginMs = v; }
    public double getMaxPayloadKg() { return maxPayloadKg; }
    public void setMaxPayloadKg(double v) { this.maxPayloadKg = v; }
    public double getMaxQkPa() { return maxQkPa; }
    public void setMaxQkPa(double v) { this.maxQkPa = v; }
    public double getInsertionTimeS() { return insertionTimeS; }
    public void setInsertionTimeS(double v) { this.insertionTimeS = v; }
    public Instant getNextWindowUtc() { return nextWindowUtc; }
    public void setNextWindowUtc(Instant v) { this.nextWindowUtc = v; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
