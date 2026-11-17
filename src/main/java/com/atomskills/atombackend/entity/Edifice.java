package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "edifice")
@Getter
@Setter
@NoArgsConstructor
public class Edifice {

    @Id
    @Column(name = "id_edifice", nullable = false)
    private Integer idEdifice;

    @Column(nullable = false, length = 120)
    private String name;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "resource_type", nullable = false, columnDefinition = "varchar[]")
    private String[] resourceType;

    @Column(length = 50)
    private String district;

    @Column(length = 80)
    private String street;

    @Column(name = "house_number", length = 10)
    private String houseNumber;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    @OneToMany(mappedBy = "edifice", fetch = FetchType.LAZY)
    private List<Event> events = new ArrayList<>();
}
