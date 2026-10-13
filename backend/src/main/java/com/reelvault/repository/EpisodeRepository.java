package com.reelvault.repository;

import com.reelvault.domain.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    java.util.Optional<Episode> findByIdAndSeriesOwnerId(Long id, Long ownerId);
    long countBySeriesId(Long seriesId);
}
