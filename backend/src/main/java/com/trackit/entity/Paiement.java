package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Getter
@Setter
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paiement")
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 10)
    private String devise;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutPaiement statut;

    @Column(name = "reference_stripe", unique = true, length = 255)
    private String referenceStripe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_abonnement", nullable = false)
    private Abonnement abonnement;

    @PrePersist
    void prePersist() {
        if (datePaiement == null) {
            datePaiement = LocalDateTime.now();
        }
    }
}
