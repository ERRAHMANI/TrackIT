package com.trackit.repository;

import com.trackit.entity.Priorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrioriteRepository extends JpaRepository<Priorite, Long> {

    List<Priorite> findAllByOrderByNiveauAsc();

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
}
