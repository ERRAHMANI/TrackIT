package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Priorité propre à une organisation (V6) : chaque client gère ses propres valeurs. */
@Entity
@Table(name = "priorite", uniqueConstraints =
        @UniqueConstraint(name = "uk_priorite_organisation_nom", columnNames = {"id_organisation", "nom"}))
@Getter
@Setter
public class Priorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_priorite")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisation", nullable = false)
    private Organisation organisation;

    @Column(nullable = false, length = 50)
    private String nom;

    @Column(nullable = false)
    private Integer niveau;

    @Column(length = 255)
    private String description;
}
