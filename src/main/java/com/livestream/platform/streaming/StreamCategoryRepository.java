package com.livestream.platform.streaming;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StreamCategoryRepository extends JpaRepository<StreamCategoryEntity, UUID> {
    List<StreamCategoryEntity> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    Optional<StreamCategoryEntity> findByIdAndActiveTrue(UUID id);
}
