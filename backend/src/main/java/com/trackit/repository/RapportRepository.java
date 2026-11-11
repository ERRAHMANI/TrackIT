package com.trackit.repository;

import com.trackit.entity.Rapport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RapportRepository extends JpaRepository<Rapport, Long> {

    @EntityGraph(attributePaths = {"analyste"})
    Page<Rapport> findByAnalysteOrganisationIdOrderByDateGenerationDesc(Long organisationId, Pageable pageable);

    @EntityGraph(attributePaths = {"analyste", "analyste.organisation"})
    Optional<Rapport> findWithAnalysteById(Long id);

    /** Vrai si l'analyste a généré au moins un rapport (suppression logique). */
    boolean existsByAnalysteId(Long analysteId);
}
