-- OhMann MySQL schema (reference).
-- Hibernate creates/updates these tables automatically (spring.jpa.hibernate.ddl-auto=update);
-- run this script only if you prefer to create the schema by hand.
-- Column names match Hibernate's default snake_case naming exactly (including the
-- slightly odd ones such as diameterm, isps and insertion_times), so the app and this
-- script agree on the same tables.

CREATE DATABASE IF NOT EXISTS ohmann;
USE ohmann;

CREATE TABLE IF NOT EXISTS launch_vehicles (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(80)  NOT NULL UNIQUE,
    manufacturer     VARCHAR(80),
    country          VARCHAR(60),
    diameterm        DOUBLE       NOT NULL,
    drag_coefficient DOUBLE       NOT NULL,
    aero_source      VARCHAR(20)  NOT NULL DEFAULT 'CONSTANT',
    drag_curve_json  TEXT
);

CREATE TABLE IF NOT EXISTS vehicle_stages (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id         BIGINT      NOT NULL,
    stage_order        INT         NOT NULL,
    name               VARCHAR(60) NOT NULL,
    propellant_mass_kg DOUBLE      NOT NULL,
    dry_mass_kg        DOUBLE      NOT NULL,
    thrust_kn          DOUBLE      NOT NULL,
    isps               DOUBLE      NOT NULL,
    CONSTRAINT fk_stage_vehicle FOREIGN KEY (vehicle_id) REFERENCES launch_vehicles (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS launch_sites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    country         VARCHAR(60),
    latitude_deg    DOUBLE NOT NULL,
    longitude_deg   DOUBLE NOT NULL,
    min_azimuth_deg DOUBLE NOT NULL,
    max_azimuth_deg DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS celestial_bodies (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                      VARCHAR(40) NOT NULL UNIQUE,
    body_type                 VARCHAR(20) NOT NULL,
    semi_major_axis_au        DOUBLE NOT NULL,
    mean_longitude_j2000_deg  DOUBLE NOT NULL,
    gm_km3s2                  DOUBLE NOT NULL,
    radius_km                 DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS app_users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(80)  NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    phone         VARCHAR(10),
    organization  VARCHAR(100),
    role          VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    password_salt VARCHAR(40)  NOT NULL,
    created_at    DATETIME(6)
);

CREATE TABLE IF NOT EXISTS mission_plans (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_name         VARCHAR(100) NOT NULL,
    created_at           DATETIME(6),
    vehicle_id           BIGINT       NOT NULL,
    site_id              BIGINT       NOT NULL,
    planned_by_id        BIGINT,
    orbit_type           VARCHAR(10)  NOT NULL,
    target_altitude_km   DOUBLE,
    inclination_deg      DOUBLE,
    payload_kg           DOUBLE,
    feasible             BIT          NOT NULL,
    launch_azimuth_deg   DOUBLE,
    pitch_kick_deg       DOUBLE,
    final_pitch_deg      DOUBLE,
    gravity_loss_ms      DOUBLE,
    drag_loss_ms         DOUBLE,
    steering_loss_ms     DOUBLE,
    ascent_dv_ms         DOUBLE,
    post_insertion_dv_ms DOUBLE,
    total_dv_ms          DOUBLE,
    dv_margin_ms         DOUBLE,
    max_payload_kg       DOUBLE,
    max_qk_pa            DOUBLE,
    insertion_times      DOUBLE,
    next_window_utc      DATETIME(6),
    notes                VARCHAR(2000),
    CONSTRAINT fk_mission_vehicle FOREIGN KEY (vehicle_id) REFERENCES launch_vehicles (id),
    CONSTRAINT fk_mission_site FOREIGN KEY (site_id) REFERENCES launch_sites (id),
    CONSTRAINT fk_mission_user FOREIGN KEY (planned_by_id) REFERENCES app_users (id)
);

CREATE TABLE IF NOT EXISTS transfer_plans (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at          DATETIME(6),
    planned_by_id       BIGINT,
    origin              VARCHAR(40) NOT NULL,
    destination         VARCHAR(40) NOT NULL,
    departure_utc       DATETIME(6),
    arrival_utc         DATETIME(6),
    time_of_flight_days DOUBLE,
    phase_angle_deg     DOUBLE,
    c3_km2s2            DOUBLE,
    departure_dv_ms     DOUBLE,
    arrival_dv_ms       DOUBLE,
    total_dv_ms         DOUBLE,
    vehicle_name        VARCHAR(80),
    payload_capacity_kg DOUBLE,
    CONSTRAINT fk_transfer_user FOREIGN KEY (planned_by_id) REFERENCES app_users (id)
);

-- Report queries (the dashboard runs similar ones through JDBC in StatsDao)
-- SELECT orbit_type, COUNT(*) FROM mission_plans GROUP BY orbit_type;
-- SELECT v.name, COUNT(m.id), AVG(m.max_payload_kg) FROM launch_vehicles v
--   LEFT JOIN mission_plans m ON m.vehicle_id = v.id GROUP BY v.id, v.name;
