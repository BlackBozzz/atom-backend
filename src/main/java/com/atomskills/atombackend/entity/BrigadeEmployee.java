package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "brigade_employee")
@Getter
@Setter
@NoArgsConstructor
@IdClass(BrigadeEmployee.Pk.class)
public class BrigadeEmployee {

    @Id
    @Column(name = "brigade_id", nullable = false)
    private Long brigadeId;

    @Id
    @Column(name = "employee_tabel", nullable = false)
    private Long employeeTabel;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long brigadeId;
        private Long employeeTabel;
    }
}
