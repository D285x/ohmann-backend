package com.anurag.ECE.controller;

import com.anurag.ECE.dto.BodyDto;
import com.anurag.ECE.service.BodyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bodies")
public class BodyController {

    private final BodyService service;

    public BodyController(BodyService service) {
        this.service = service;
    }

    @GetMapping
    public List<BodyDto> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<BodyDto> create(@Valid @RequestBody BodyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public BodyDto update(@PathVariable Long id, @Valid @RequestBody BodyDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
