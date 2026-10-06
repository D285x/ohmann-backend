package com.anurag.ECE.controller;

import com.anurag.ECE.dto.AeroRefineRequest;
import com.anurag.ECE.dto.VehicleDto;
import com.anurag.ECE.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehicleDto> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public VehicleDto get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<VehicleDto> create(@Valid @RequestBody VehicleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public VehicleDto update(@PathVariable Long id, @Valid @RequestBody VehicleDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fetches a Mach-indexed drag coefficient curve from the aero-service
     * (shockFLOW GPU CFD, or its analytic fallback) and stores it on the
     * vehicle. Returns 503 (via {@code AeroServiceException}) if the
     * aero-service is not running - the vehicle is left unchanged.
     */
    @PostMapping("/{id}/aero/refine")
    public VehicleDto refineAero(@PathVariable Long id,
                                  @Valid @RequestBody(required = false) AeroRefineRequest req) {
        return service.refineAero(id, req);
    }

    @DeleteMapping("/{id}/aero")
    public VehicleDto resetAero(@PathVariable Long id) {
        return service.resetAero(id);
    }
}
