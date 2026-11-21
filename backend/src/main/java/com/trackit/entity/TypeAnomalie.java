package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Type d'anomalie propre à une organisation (V6) : chaque client gère ses propres valeurs. */
@Entity
@Table(name = "type_anomalie", uniqueConstraints =
        @UniqueConstraint(name = "uk_type_anomalie_organisation_nom", columnNames = {"id_organisation", "nom"}))
@Getter
@Setter
public class TypeAnomalie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_type")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisation", nullable = false)
    private Organisation organisation;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 255)
    private String description;
}
