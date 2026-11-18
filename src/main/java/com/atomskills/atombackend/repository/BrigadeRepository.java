package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.Brigade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrigadeRepository extends JpaRepository<Brigade, Long> {
}
