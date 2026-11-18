package com.atomskills.atombackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brigade")
@Getter
@Setter
@NoArgsConstructor
public class Brigade {
    @Id
    @Column(name = "id")
    private Long id;
}