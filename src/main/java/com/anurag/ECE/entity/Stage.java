package com.anurag.ECE.entity;

import jakarta.persistence.*;

/** One sequential stage of a launch vehicle (strap-on boosters are merged into stage 1). */
@Entity
@Table(name = "vehicle_stages")
public class Stage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int stageOrder;

    @Column(nullable = false, length = 60)
    private String name;

    private double propellantMassKg;
    private double dryMassKg;
    private double thrustKn;
    private double ispS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private LaunchVehicle vehicle;

    public Stage() {
    }

    public Stage(String name, double propellantMassKg, double dryMassKg, double thrustKn, double ispS) {
        this.name = name;
        this.propellantMassKg = propellantMassKg;
        this.dryMassKg = dryMassKg;
        this.thrustKn = thrustKn;
        this.ispS = ispS;
    }

    public Long getId() { return id; }
    public int getStageOrder() { return stageOrder; }
    public void setStageOrder(int stageOrder) { this.stageOrder = stageOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPropellantMassKg() { return propellantMassKg; }
    public void setPropellantMassKg(double v) { this.propellantMassKg = v; }
    public double getDryMassKg() { return dryMassKg; }
    public void setDryMassKg(double v) { this.dryMassKg = v; }
    public double getThrustKn() { return thrustKn; }
    public void setThrustKn(double v) { this.thrustKn = v; }
    public double getIspS() { return ispS; }
    public void setIspS(double v) { this.ispS = v; }
    public LaunchVehicle getVehicle() { return vehicle; }
    public void setVehicle(LaunchVehicle vehicle) { this.vehicle = vehicle; }
}
