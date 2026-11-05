package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rapport")
@Getter
@Setter
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rapport")
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(nullable = false, length = 100)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatRapport format;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @Column(name = "chemin_fichier", length = 500)
    private String cheminFichier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_analyste", nullable = false)
    private Utilisateur analyste;

    @PrePersist
    void prePersist() {
        if (dateGeneration == null) {
            dateGeneration = LocalDateTime.now();
        }
    }
}
