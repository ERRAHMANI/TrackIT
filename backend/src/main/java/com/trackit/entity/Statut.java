package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "statut")
@Getter
@Setter
public class Statut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_statut")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(length = 255)
    private String description;

    public StatutCode getCode() {
        return StatutCode.fromLibelle(nom);
    }
}
