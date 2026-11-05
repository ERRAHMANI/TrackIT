package com.trackit.repository;

import com.trackit.entity.Statut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatutRepository extends JpaRepository<Statut, Long> {

    Optional<Statut> findByNom(String nom);
}
