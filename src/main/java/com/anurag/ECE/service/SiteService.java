package com.anurag.ECE.service;

import com.anurag.ECE.dto.SiteDto;
import com.anurag.ECE.entity.LaunchSite;
import com.anurag.ECE.exception.ConflictException;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.repository.LaunchSiteRepository;
import com.anurag.ECE.repository.MissionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SiteService {

    private final LaunchSiteRepository repo;
    private final MissionPlanRepository missions;

    public SiteService(LaunchSiteRepository repo, MissionPlanRepository missions) {
        this.repo = repo;
        this.missions = missions;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> findAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(LaunchSite::getName, String.CASE_INSENSITIVE_ORDER))
                .map(Mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteDto findById(Long id) {
        return Mapper.toDto(getEntity(id));
    }

    public LaunchSite getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Launch site", id));
    }

    @Transactional
    public SiteDto create(SiteDto dto) {
        if (repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A site named '" + dto.name() + "' already exists");
        }
        LaunchSite s = new LaunchSite();
        Mapper.copy(dto, s);
        return Mapper.toDto(repo.save(s));
    }

    @Transactional
    public SiteDto update(Long id, SiteDto dto) {
        LaunchSite s = getEntity(id);
        if (!s.getName().equalsIgnoreCase(dto.name().trim()) && repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A site named '" + dto.name() + "' already exists");
        }
        Mapper.copy(dto, s);
        return Mapper.toDto(repo.save(s));
    }

    @Transactional
    public void delete(Long id) {
        LaunchSite s = getEntity(id);
        if (missions.existsBySiteId(id)) {
            throw new ConflictException("Site is used by saved missions; delete those missions first");
        }
        repo.delete(s);
    }
}
