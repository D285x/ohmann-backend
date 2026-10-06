package com.anurag.ECE.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** A saved interplanetary Hohmann transfer plan. */
@Entity
@Table(name = "transfer_plans")
public class TransferPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt = Instant.now();

    @Column(length = 40, nullable = false)
    private String origin;

    @Column(length = 40, nullable = false)
    private String destination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "planned_by_id")
    private AppUser plannedBy;

    private Instant departureUtc;
    private Instant arrivalUtc;
    private double timeOfFlightDays;
    private double phaseAngleDeg;
    private double c3Km2s2;
    private double departureDvMs;
    private double arrivalDvMs;
    private double totalDvMs;

    @Column(length = 80)
    private String vehicleName;

    /** Estimated payload the selected vehicle can inject (null if no vehicle chosen). */
    private Double payloadCapacityKg;

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public AppUser getPlannedBy() { return plannedBy; }
    public void setPlannedBy(AppUser plannedBy) { this.plannedBy = plannedBy; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Instant getDepartureUtc() { return departureUtc; }
    public void setDepartureUtc(Instant v) { this.departureUtc = v; }
    public Instant getArrivalUtc() { return arrivalUtc; }
    public void setArrivalUtc(Instant v) { this.arrivalUtc = v; }
    public double getTimeOfFlightDays() { return timeOfFlightDays; }
    public void setTimeOfFlightDays(double v) { this.timeOfFlightDays = v; }
    public double getPhaseAngleDeg() { return phaseAngleDeg; }
    public void setPhaseAngleDeg(double v) { this.phaseAngleDeg = v; }
    public double getC3Km2s2() { return c3Km2s2; }
    public void setC3Km2s2(double v) { this.c3Km2s2 = v; }
    public double getDepartureDvMs() { return departureDvMs; }
    public void setDepartureDvMs(double v) { this.departureDvMs = v; }
    public double getArrivalDvMs() { return arrivalDvMs; }
    public void setArrivalDvMs(double v) { this.arrivalDvMs = v; }
    public double getTotalDvMs() { return totalDvMs; }
    public void setTotalDvMs(double v) { this.totalDvMs = v; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String v) { this.vehicleName = v; }
    public Double getPayloadCapacityKg() { return payloadCapacityKg; }
    public void setPayloadCapacityKg(Double v) { this.payloadCapacityKg = v; }
}
