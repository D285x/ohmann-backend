package com.anurag.ECE.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "launch_vehicles")
public class LaunchVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 80)
    private String manufacturer;

    @Column(length = 60)
    private String country;

    /** Body diameter in metres, used for aerodynamic reference area. */
    private double diameterM;

    /** Average drag coefficient during ascent (used when no Mach-dependent curve is available). */
    private double dragCoefficient;

    /** CONSTANT (default), ANALYTIC_FALLBACK, or SHOCKFLOW. See {@link com.anurag.ECE.aero.AeroClient}. */
    @Column(length = 20, nullable = false)
    private String aeroSource = "CONSTANT";

    /** JSON array of {"mach":..,"cd":..} points, present only after an aero refine. */
    // Plain TEXT column (no @Lob) so the same mapping works on MySQL, H2 and PostgreSQL
    @Column(columnDefinition = "TEXT")
    private String dragCurveJson;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stageOrder ASC")
    private List<Stage> stages = new ArrayList<>();

    public LaunchVehicle() {
    }

    public LaunchVehicle(String name, String manufacturer, String country, double diameterM, double dragCoefficient) {
        this.name = name;
        this.manufacturer = manufacturer;
        this.country = country;
        this.diameterM = diameterM;
        this.dragCoefficient = dragCoefficient;
    }

    public void addStage(Stage stage) {
        stage.setVehicle(this);
        stage.setStageOrder(stages.size() + 1);
        stages.add(stage);
    }

    public void replaceStages(List<Stage> newStages) {
        stages.clear();
        newStages.forEach(this::addStage);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public double getDiameterM() { return diameterM; }
    public void setDiameterM(double diameterM) { this.diameterM = diameterM; }
    public double getDragCoefficient() { return dragCoefficient; }
    public void setDragCoefficient(double dragCoefficient) { this.dragCoefficient = dragCoefficient; }
    public String getAeroSource() { return aeroSource; }
    public void setAeroSource(String aeroSource) { this.aeroSource = aeroSource; }
    public String getDragCurveJson() { return dragCurveJson; }
    public void setDragCurveJson(String dragCurveJson) { this.dragCurveJson = dragCurveJson; }
    public List<Stage> getStages() { return stages; }
}
