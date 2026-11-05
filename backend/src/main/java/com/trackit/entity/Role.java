package com.trackit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(length = 255)
    private String description;

    public RoleCode getCode() {
        return RoleCode.fromLibelle(nom);
    }
}
