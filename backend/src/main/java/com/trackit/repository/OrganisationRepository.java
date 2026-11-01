package com.trackit.repository;

import com.trackit.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
