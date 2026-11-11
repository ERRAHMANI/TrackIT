package com.trackit.repository;

import com.trackit.entity.Historique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Long> {

    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.role"})
    List<Historique> findByAnomalieIdOrderByDateModificationAscIdAsc(Long anomalieId);

    long countByAnomalieId(Long anomalieId);

    /** Vrai si l'utilisateur apparaît dans l'historique (suppression logique). */
    boolean existsByUtilisateurId(Long utilisateurId);
}
