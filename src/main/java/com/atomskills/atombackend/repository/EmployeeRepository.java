package com.atomskills.atombackend.repository;

import com.atomskills.atombackend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}