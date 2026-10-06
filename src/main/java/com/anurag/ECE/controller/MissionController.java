package com.anurag.ECE.controller;

import com.anurag.ECE.dto.MissionRequest;
import com.anurag.ECE.dto.MissionResponse;
import com.anurag.ECE.security.SessionStore;
import com.anurag.ECE.service.MissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService service;
    private final SessionStore sessions;

    public MissionController(MissionService service, SessionStore sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    /** Runs the trajectory optimization; the plan is stored only when {@code save} is true. */
    @PostMapping("/plan")
    public MissionResponse plan(@Valid @RequestBody MissionRequest request) {
        return service.plan(request);
    }

    @GetMapping
    public List<MissionResponse> history() {
        return service.history();
    }

    @GetMapping("/{id}")
    public MissionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        Path file = service.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.readFile(file));
    }

    /** Only the operator who saved the plan may delete it (needs a session token). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        service.delete(id, sessions.requireUser(auth));
        return ResponseEntity.noContent().build();
    }
}
