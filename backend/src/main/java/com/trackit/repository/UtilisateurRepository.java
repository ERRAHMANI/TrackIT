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

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"role"})
    List<Utilisateur> findByOrganisationIdAndRoleNomAndActifTrueOrderByNomAscPrenomAsc(Long organisationId, String roleNom);
}
