package com.anurag.ECE.aero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client for the OhMann aero-service (see {@code /aero-service} at the repo
 * root): a small Python process that wraps shockFLOW, a GPU-based transonic CFD
 * solver, to produce a Mach-indexed drag coefficient curve for a vehicle's nose
 * shape. It is called once per "refine aerodynamics" action, never from inside
 * the ascent simulator's hot loop (a CFD sweep can take seconds to minutes; the
 * simulator/optimizer runs thousands of steps per second and needs the cached,
 * already-fetched curve stored on the vehicle instead).
 * <p>
 * If the aero-service is unreachable, times out, or errors, callers should catch
 * {@link AeroServiceException} and fall back to the vehicle's constant drag
 * coefficient - which is exactly what happens if a refine is simply never run.
 */
@Component
public class AeroClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final Duration requestTimeout;

    public AeroClient(@Value("${ohmann.aero.service-url:http://localhost:8500}") String baseUrl,
                       @Value("${ohmann.aero.timeout-seconds:180}") long timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    public AeroCurveResult fetchDragCurve(double diameterM, double noseFinenessRatio,
                                          double machMin, double machMax, int points) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("diameterM", diameterM);
        body.put("noseFinenessRatio", noseFinenessRatio);
        body.put("machMin", machMin);
        body.put("machMax", machMax);
        body.put("points", points);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/drag-curve"))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
        } catch (IOException e) {
            throw new AeroServiceException("Could not build aero-service request: " + e.getMessage(), e);
        }

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new AeroServiceException(
                    "Could not reach the aero-service at " + baseUrl + " (is it running? see aero-service/README.md): "
                            + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AeroServiceException("Aero-service call was interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new AeroServiceException("Aero-service returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            JsonNode machNode = root.get("mach");
            JsonNode cdNode = root.get("cd");
            if (machNode == null || cdNode == null || !machNode.isArray() || !cdNode.isArray()
                    || machNode.size() != cdNode.size() || machNode.isEmpty()) {
                throw new AeroServiceException("Aero-service response did not contain a matching mach/cd curve");
            }
            double[] mach = new double[machNode.size()];
            double[] cd = new double[cdNode.size()];
            for (int i = 0; i < mach.length; i++) {
                mach[i] = machNode.get(i).asDouble();
                cd[i] = cdNode.get(i).asDouble();
            }
            String source = root.has("source") ? root.get("source").asText() : "unknown";
            return new AeroCurveResult(mach, cd, source);
        } catch (IOException e) {
            throw new AeroServiceException("Could not parse aero-service response: " + e.getMessage(), e);
        }
    }
}
