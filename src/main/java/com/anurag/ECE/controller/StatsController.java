package com.anurag.ECE.controller;

import com.anurag.ECE.dao.StatsDao;
import com.anurag.ECE.dto.StatsDto;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsDao dao;

    public StatsController(StatsDao dao) {
        this.dao = dao;
    }

    @GetMapping
    public StatsDto stats() {
        return dao.loadStats();
    }

    @DeleteMapping("/missions")
    public Map<String, Integer> purge(@RequestParam(defaultValue = "30") int olderThanDays) {
        return Map.of("deleted", dao.purgeMissionsOlderThan(olderThanDays));
    }
}
