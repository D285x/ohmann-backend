package com.anurag.ECE.controller;

import com.anurag.ECE.dto.TransferRequest;
import com.anurag.ECE.dto.TransferResponse;
import com.anurag.ECE.dto.TransferSummary;
import com.anurag.ECE.security.SessionStore;
import com.anurag.ECE.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final TransferService service;
    private final SessionStore sessions;

    public TransferController(TransferService service, SessionStore sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    @PostMapping("/transfers/plan")
    public TransferResponse plan(@Valid @RequestBody TransferRequest request) {
        return service.plan(request);
    }

    @GetMapping("/transfers")
    public List<TransferSummary> history() {
        return service.history();
    }

    /** Only the operator who saved the plan may delete it (needs a session token). */
    @DeleteMapping("/transfers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        service.delete(id, sessions.requireUser(auth));
        return ResponseEntity.noContent().build();
    }
}
