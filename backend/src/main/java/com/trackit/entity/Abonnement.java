package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "abonnement")
@Getter
@Setter
public class Abonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_abonnement")
    private Long id;

    @Column(nullable = false, length = 100)
    private String offre;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutAbonnement statut;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisation", nullable = false)
    private Organisation organisation;

    public boolean estActifLe(LocalDate jour) {
        return statut == StatutAbonnement.ACTIF
                && !dateDebut.isAfter(jour)
                && (dateFin == null || !dateFin.isBefore(jour));
    }
}
