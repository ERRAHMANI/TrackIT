package com.trackit.repository;

import com.trackit.entity.SystemeIt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemeItRepository extends JpaRepository<SystemeIt, Long> {

    List<SystemeIt> findByOrganisationIdOrderByNomAsc(Long organisationId);

    Optional<SystemeIt> findByIdAndOrganisationId(Long id, Long organisationId);

    boolean existsByOrganisationIdAndNomIgnoreCase(Long organisationId, String nom);

    boolean existsByOrganisationIdAndNomIgnoreCaseAndIdNot(Long organisationId, String nom, Long id);
}
