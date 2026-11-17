package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStatusRepository extends JpaRepository<EventStatus, Short> { }
