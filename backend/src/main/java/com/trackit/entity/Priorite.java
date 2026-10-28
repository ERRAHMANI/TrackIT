package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "priorite")
@Getter
@Setter
public class Priorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_priorite")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(nullable = false)
    private Integer niveau;

    @Column(length = 255)
    private String description;
}
