package com.trackit.repository;

import com.trackit.entity.Priorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Priorités : toujours lues et vérifiées dans l'organisation de l'utilisateur connecté. */
public interface PrioriteRepository extends JpaRepository<Priorite, Long> {

    List<Priorite> findByOrganisationIdOrderByNiveauAsc(Long organisationId);

    Optional<Priorite> findByIdAndOrganisationId(Long id, Long organisationId);

    boolean existsByOrganisationIdAndNomIgnoreCase(Long organisationId, String nom);

    boolean existsByOrganisationIdAndNomIgnoreCaseAndIdNot(Long organisationId, String nom, Long id);
}
