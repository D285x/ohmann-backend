package com.anurag.ECE.util;

import com.anurag.ECE.physics.TrajectoryPoint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Writes trajectory samples to CSV files. */
public final class CsvExporter {

    public static final String HEADER =
            "time_s,altitude_km,downrange_km,velocity_ms,flight_path_deg,pitch_deg,mass_kg,dynamic_pressure_kpa,stage";

    private CsvExporter() {
    }

    public static Path write(List<TrajectoryPoint> points, Path directory, String baseName) throws IOException {
        Files.createDirectories(directory);
        String safe = baseName.replaceAll("[^A-Za-z0-9_-]", "_");
        Path file = directory.resolve(safe + ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (TrajectoryPoint p : points) {
                w.write(String.format(Locale.ROOT, "%.1f,%.3f,%.3f,%.1f,%.2f,%.2f,%.1f,%.3f,%d",
                        p.timeS(), p.altitudeKm(), p.downrangeKm(), p.velocityMs(), p.flightPathDeg(),
                        p.pitchDeg(), p.massKg(), p.dynamicPressureKPa(), p.stage()));
                w.newLine();
            }
        }
        return file;
    }
}
