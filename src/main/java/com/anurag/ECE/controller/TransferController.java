package com.anurag.ECE.controller;

import com.anurag.ECE.dto.TransferRequest;
import com.anurag.ECE.dto.TransferResponse;
import com.anurag.ECE.dto.TransferSummary;
import com.anurag.ECE.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping("/transfers/plan")
    public TransferResponse plan(@Valid @RequestBody TransferRequest request) {
        return service.plan(request);
    }

    @GetMapping("/transfers")
    public List<TransferSummary> history() {
        return service.history();
    }

    @DeleteMapping("/transfers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
