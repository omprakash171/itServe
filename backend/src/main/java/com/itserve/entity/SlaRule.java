package com.itserve.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sla_rules")
@Getter
@Setter
public class SlaRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private Priority priority;

    /** Target resolution time in hours for this priority. */
    @Column(name = "resolution_hours", nullable = false)
    private int resolutionHours;

    public SlaRule() {
    }

    public SlaRule(Priority priority, int resolutionHours) {
        this.priority = priority;
        this.resolutionHours = resolutionHours;
    }
}
