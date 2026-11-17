package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Integer> { }
