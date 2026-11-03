package com.trackit.repository;

import com.trackit.entity.Paiement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    @EntityGraph(attributePaths = {"abonnement", "abonnement.organisation"})
    Optional<Paiement> findByReferenceStripe(String referenceStripe);

    @EntityGraph(attributePaths = {"abonnement"})
    List<Paiement> findByAbonnementOrganisationIdOrderByDatePaiementDesc(Long organisationId);
}
