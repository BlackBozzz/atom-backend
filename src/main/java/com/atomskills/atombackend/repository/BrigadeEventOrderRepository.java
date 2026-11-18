package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.BrigadeEventOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrigadeEventOrderRepository extends JpaRepository<BrigadeEventOrder, BrigadeEventOrder.Pk> {

    @Modifying
    @Query("DELETE FROM BrigadeEventOrder eo WHERE eo.brigadeId = :brigadeId")
    void deleteByBrigadeId(@Param("brigadeId") Long brigadeId);
}