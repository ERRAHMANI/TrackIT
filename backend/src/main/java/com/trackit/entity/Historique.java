package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique")
@Getter
@Setter
public class Historique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historique")
    private Long id;

    @Column(nullable = false, length = 255)
    private String action;

    @Column(name = "ancien_statut", length = 50)
    private String ancienStatut;

    @Column(name = "nouveau_statut", length = 50)
    private String nouveauStatut;

    @Column(name = "ancienne_priorite", length = 50)
    private String anciennePriorite;

    @Column(name = "nouvelle_priorite", length = 50)
    private String nouvellePriorite;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_anomalie", nullable = false)
    private Anomalie anomalie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    void prePersist() {
        if (dateModification == null) {
            dateModification = LocalDateTime.now();
        }
    }
}
