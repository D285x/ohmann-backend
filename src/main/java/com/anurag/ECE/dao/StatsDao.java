package com.anurag.ECE.dao;

import com.anurag.ECE.dto.StatsDto;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard reporting queries written with plain JDBC (PreparedStatement and ResultSet).
 * The connection comes from Spring's pooled DataSource so it works with both MySQL and H2.
 */
@Repository
public class StatsDao {

    private final DataSource dataSource;

    public StatsDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public StatsDto loadStats() {
        try (Connection con = dataSource.getConnection()) {
            long vehicles = count(con, "SELECT COUNT(*) FROM launch_vehicles");
            long sites = count(con, "SELECT COUNT(*) FROM launch_sites");
            long bodies = count(con, "SELECT COUNT(*) FROM celestial_bodies");
            long operators = count(con, "SELECT COUNT(*) FROM app_users");
            long missions = count(con, "SELECT COUNT(*) FROM mission_plans");
            long feasible = countWhere(con, "SELECT COUNT(*) FROM mission_plans WHERE feasible = ?", true);
            long transfers = count(con, "SELECT COUNT(*) FROM transfer_plans");
            return new StatsDto(vehicles, sites, bodies, operators, missions, feasible, transfers,
                    missionsByOrbit(con), vehicleStats(con));
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load statistics: " + e.getMessage(), e);
        }
    }

    private long count(Connection con, String sql) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private long countWhere(Connection con, String sql, boolean flag) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, flag);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private Map<String, Long> missionsByOrbit(Connection con) throws SQLException {
        Map<String, Long> map = new LinkedHashMap<>();
        String sql = "SELECT orbit_type, COUNT(*) AS n FROM mission_plans GROUP BY orbit_type ORDER BY orbit_type";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("orbit_type"), rs.getLong("n"));
            }
        }
        return map;
    }

    private List<StatsDto.VehicleStat> vehicleStats(Connection con) throws SQLException {
        String sql = """
                SELECT v.name AS vehicle, COUNT(m.id) AS n,
                       COALESCE(AVG(m.max_payload_kg), 0) AS avg_payload,
                       COALESCE(MAX(m.dv_margin_ms), 0) AS best_margin
                FROM launch_vehicles v
                LEFT JOIN mission_plans m ON m.vehicle_id = v.id
                GROUP BY v.id, v.name
                ORDER BY n DESC, v.name
                """;
        List<StatsDto.VehicleStat> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new StatsDto.VehicleStat(rs.getString("vehicle"), rs.getLong("n"),
                        rs.getDouble("avg_payload"), rs.getDouble("best_margin")));
            }
        }
        return list;
    }

    /** Deletes saved missions older than the given number of days; returns rows affected. */
    public int purgeMissionsOlderThan(int days) {
        String sql = "DELETE FROM mission_plans WHERE created_at < ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(java.time.Instant.now().minusSeconds(days * 86_400L)));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Purge failed: " + e.getMessage(), e);
        }
    }
}
