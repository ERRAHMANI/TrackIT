package com.trackit.repository;

import com.trackit.entity.Commentaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    @EntityGraph(attributePaths = {"utilisateur", "utilisateur.role"})
    List<Commentaire> findByAnomalieIdOrderByDateCreationAsc(Long anomalieId);

    long countByAnomalieId(Long anomalieId);
}
