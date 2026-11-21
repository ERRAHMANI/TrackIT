package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "mot_de_passe", nullable = false, length = 255)
    private String motDePasse;

    /** Instant du dernier changement de mot de passe : les jetons émis avant sont refusés (V7). */
    @Column(name = "mot_de_passe_modifie_le", nullable = false)
    private LocalDateTime motDePasseModifieLe;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    /** Renseignée quand le compte a été supprimé logiquement (anonymisé). */
    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisation", nullable = false)
    private Organisation organisation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (motDePasseModifieLe == null) {
            motDePasseModifieLe = maintenantALaMilliseconde();
        }
    }

    /**
     * Remplace le mot de passe (empreinte BCrypt déjà calculée) et date le changement :
     * tous les jetons émis auparavant pour ce compte deviennent invalides.
     */
    public void changerMotDePasse(String empreinte) {
        this.motDePasse = empreinte;
        this.motDePasseModifieLe = maintenantALaMilliseconde();
    }

    /** Tronqué à la milliseconde, comme la colonne DATETIME(3) : MySQL arrondirait sinon vers le haut. */
    private static LocalDateTime maintenantALaMilliseconde() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public String getNomComplet() {
        return prenom + " " + nom;
    }

    public RoleCode getRoleCode() {
        return role.getCode();
    }

    public boolean isSupprime() {
        return dateSuppression != null;
    }
}
