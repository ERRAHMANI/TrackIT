package com.trackit.repository;

import com.trackit.entity.PieceJointe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PieceJointeRepository extends JpaRepository<PieceJointe, Long> {

    @EntityGraph(attributePaths = {"utilisateur"})
    List<PieceJointe> findByAnomalieIdOrderByDateAjoutAsc(Long anomalieId);

    @EntityGraph(attributePaths = {"anomalie", "anomalie.systeme"})
    Optional<PieceJointe> findWithAnomalieById(Long id);

    long countByAnomalieId(Long anomalieId);

    /** Vrai si l'utilisateur a ajouté au moins une pièce jointe (suppression logique). */
    boolean existsByUtilisateurId(Long utilisateurId);
}
