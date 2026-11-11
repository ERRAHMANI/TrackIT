package com.trackit.repository;

import com.trackit.entity.Utilisateur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long>, JpaSpecificationExecutor<Utilisateur> {

    @EntityGraph(attributePaths = {"role", "organisation"})
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role", "organisation"})
    Optional<Utilisateur> findWithRoleById(Long id);

    @EntityGraph(attributePaths = {"role", "organisation"})
    Optional<Utilisateur> findByIdAndOrganisationId(Long id, Long organisationId);

    /** Compte de l'organisation qui n'a pas été supprimé logiquement. */
    @EntityGraph(attributePaths = {"role", "organisation"})
    Optional<Utilisateur> findByIdAndOrganisationIdAndDateSuppressionIsNull(Long id, Long organisationId);

    /** Nombre d'autres comptes actifs ayant ce rôle dans l'organisation (protection du dernier administrateur). */
    long countByOrganisationIdAndRoleNomAndActifTrueAndIdNot(Long organisationId, String roleNom, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"role"})
    List<Utilisateur> findByOrganisationIdAndRoleNomAndActifTrueOrderByNomAscPrenomAsc(Long organisationId, String roleNom);
}
