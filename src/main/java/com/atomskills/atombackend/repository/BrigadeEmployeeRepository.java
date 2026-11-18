package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.BrigadeEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrigadeEmployeeRepository extends JpaRepository<BrigadeEmployee, BrigadeEmployee.Pk> {

    @Modifying
    @Query("DELETE FROM BrigadeEmployee be WHERE be.brigadeId = :brigadeId")
    void deleteByBrigadeId(@Param("brigadeId") Long brigadeId);
}
