package com.trackit.repository;

import com.trackit.entity.TypeAnomalie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TypeAnomalieRepository extends JpaRepository<TypeAnomalie, Long> {

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
