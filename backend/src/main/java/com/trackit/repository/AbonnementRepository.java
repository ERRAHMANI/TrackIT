package com.trackit.repository;

import com.trackit.entity.Abonnement;
import com.trackit.entity.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    List<Abonnement> findByOrganisationIdOrderByDateDebutDescIdDesc(Long organisationId);

    List<Abonnement> findByOrganisationIdAndStatut(Long organisationId, StatutAbonnement statut);
}
