package com.trackit.repository;

import com.trackit.entity.TypeAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Types d'anomalie : toujours lus et vérifiés dans l'organisation de l'utilisateur connecté. */
public interface TypeAnomalieRepository extends JpaRepository<TypeAnomalie, Long> {

    List<TypeAnomalie> findByOrganisationIdOrderByNomAsc(Long organisationId);

    Optional<TypeAnomalie> findByIdAndOrganisationId(Long id, Long organisationId);

    boolean existsByOrganisationIdAndNomIgnoreCase(Long organisationId, String nom);

    boolean existsByOrganisationIdAndNomIgnoreCaseAndIdNot(Long organisationId, String nom, Long id);
}
