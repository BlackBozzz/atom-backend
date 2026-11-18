package com.atomskills.atombackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "brigade_event_order")
@Getter
@Setter
@NoArgsConstructor
@IdClass(BrigadeEventOrder.Pk.class)
public class BrigadeEventOrder {

    @Id
    @Column(name = "brigade_id", nullable = false)
    private Long brigadeId;

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements Serializable {
        private Long brigadeId;
        private Long eventId;
    }
}