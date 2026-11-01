package com.trackit.repository;

import com.trackit.dto.JourValeur;
import com.trackit.dto.LibelleValeur;
import com.trackit.entity.Anomalie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnomalieRepository extends JpaRepository<Anomalie, Long>, JpaSpecificationExecutor<Anomalie> {

    @Override
    @EntityGraph(attributePaths = {"auteur", "operateur", "systeme", "type", "priorite", "statut"})
    Page<Anomalie> findAll(Specification<Anomalie> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"auteur", "auteur.role", "operateur", "operateur.role", "systeme",
            "systeme.organisation", "type", "priorite", "statut"})
    Optional<Anomalie> findDetailById(Long id);

    boolean existsBySystemeId(Long systemeId);

    long countByStatutNomIn(Collection<String> statuts);

    boolean existsByAuteurIdOrOperateurId(Long auteurId, Long operateurId);

    @EntityGraph(attributePaths = {"operateur", "systeme", "priorite", "statut"})
    @Query("""
            select a from Anomalie a
            where a.systeme.organisation.id = :org and a.statut.nom in :statuts
            order by a.dateCreation asc""")
    List<Anomalie> findOuvertes(@Param("org") Long organisationId, @Param("statuts") Collection<String> statuts);

    // --- Agrégations statistiques (calculées en base) ---

    @Query("""
            select new com.trackit.dto.LibelleValeur(a.statut.nom, count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by a.statut.nom""")
    List<LibelleValeur> compterParStatut(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                         @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.trackit.dto.LibelleValeur(a.type.nom, count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by a.type.nom order by count(a) desc""")
    List<LibelleValeur> compterParType(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.trackit.dto.LibelleValeur(a.priorite.nom, count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by a.priorite.nom, a.priorite.niveau order by a.priorite.niveau desc""")
    List<LibelleValeur> compterParPriorite(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                           @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.trackit.dto.LibelleValeur(a.systeme.nom, count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by a.systeme.nom order by count(a) desc""")
    List<LibelleValeur> compterParSysteme(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                          @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.trackit.dto.JourValeur(cast(a.dateCreation as LocalDate), count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by cast(a.dateCreation as LocalDate)""")
    List<JourValeur> compterCreationsParJour(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                             @Param("fin") LocalDateTime fin);

    @Query("""
            select new com.trackit.dto.JourValeur(cast(a.dateResolution as LocalDate), count(a)) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateResolution >= :debut and a.dateResolution < :fin
            group by cast(a.dateResolution as LocalDate)""")
    List<JourValeur> compterResolutionsParJour(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                               @Param("fin") LocalDateTime fin);

    /** Anomalies déclarées avant la date et non encore résolues à cette date. */
    @Query("""
            select count(a) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation < :date
              and (a.dateResolution is null or a.dateResolution >= :date)""")
    long compterNonResoluesA(@Param("org") Long org, @Param("date") LocalDateTime date);

    @Query("""
            select count(a) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateResolution >= :debut and a.dateResolution < :fin""")
    long compterResolues(@Param("org") Long org, @Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    /**
     * Par priorité, pour les anomalies résolues sur la période : nom de la priorité,
     * durée moyenne de résolution en minutes et nombre d'anomalies résolues.
     */
    @Query(value = """
            SELECT p.nom, AVG(TIMESTAMPDIFF(MINUTE, a.date_creation, a.date_resolution)), COUNT(*)
            FROM anomalie a
            JOIN systeme_it s ON s.id_systeme = a.id_systeme
            JOIN priorite p ON p.id_priorite = a.id_priorite
            WHERE s.id_organisation = :org AND a.date_resolution >= :debut AND a.date_resolution < :fin
            GROUP BY p.nom, p.niveau
            ORDER BY p.niveau DESC""", nativeQuery = true)
    List<Object[]> dureeMoyenneResolutionParPriorite(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                                     @Param("fin") LocalDateTime fin);

    /** Couples système / type les plus fréquents : incidents récurrents. */
    @Query("""
            select a.systeme.nom, a.type.nom, count(a) from Anomalie a
            where a.systeme.organisation.id = :org and a.dateCreation >= :debut and a.dateCreation < :fin
            group by a.systeme.nom, a.type.nom
            having count(a) >= :seuil
            order by count(a) desc""")
    List<Object[]> incidentsRecurrents(@Param("org") Long org, @Param("debut") LocalDateTime debut,
                                       @Param("fin") LocalDateTime fin, @Param("seuil") long seuil);

    /** Charge des opérateurs : anomalies ouvertes affectées, par statut. */
    @Query("""
            select a.operateur.id, a.operateur.prenom, a.operateur.nom, a.statut.nom, count(a) from Anomalie a
            where a.systeme.organisation.id = :org and a.operateur is not null and a.statut.nom in :statuts
            group by a.operateur.id, a.operateur.prenom, a.operateur.nom, a.statut.nom""")
    List<Object[]> chargeParOperateur(@Param("org") Long org, @Param("statuts") Collection<String> statuts);

    @Query("""
            select count(a) from Anomalie a
            where a.systeme.organisation.id = :org and a.statut.nom in :statuts and a.operateur is null""")
    long compterOuvertesNonAffectees(@Param("org") Long org, @Param("statuts") Collection<String> statuts);
}
