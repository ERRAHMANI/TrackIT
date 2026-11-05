package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "piece_jointe")
@Getter
@Setter
public class PieceJointe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piece_jointe")
    private Long id;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(nullable = false, length = 500)
    private String chemin;

    @Column(name = "type_fichier", nullable = false, length = 100)
    private String typeFichier;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_anomalie", nullable = false)
    private Anomalie anomalie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @PrePersist
    void prePersist() {
        if (dateAjout == null) {
            dateAjout = LocalDateTime.now();
        }
    }
}
