package com.trackit.repository;

import com.trackit.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUtilisateurIdOrderByDateCreationDesc(Long utilisateurId, Pageable pageable);

    Optional<Notification> findByIdAndUtilisateurId(Long id, Long utilisateurId);

    long countByUtilisateurIdAndLuFalse(Long utilisateurId);

    @Modifying
    @Query("delete from Notification n where n.utilisateur.id = :utilisateurId")
    void deleteByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

    @Modifying
    @Query("update Notification n set n.lu = true where n.utilisateur.id = :utilisateurId and n.lu = false")
    int marquerToutesLues(@Param("utilisateurId") Long utilisateurId);
}
