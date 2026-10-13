package com.reelvault.repository;

import com.reelvault.domain.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import com.reelvault.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, Long> {
    boolean existsByTitleIgnoreCase(String title);
    List<com.reelvault.domain.Media> findAllByOwnerId(Long ownerId);
    Optional<com.reelvault.domain.Media> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
    Optional<Media> findByOwnerIdAndProviderAndExternalProviderId(Long ownerId, String provider, String externalProviderId);

    @Modifying
    @Query("update Media media set media.owner = :owner where media.owner is null")
    int claimLegacyLibrary(@Param("owner") AppUser owner);
}
