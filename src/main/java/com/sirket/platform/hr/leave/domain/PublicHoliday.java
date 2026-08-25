package com.sirket.platform.hr.leave.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** FR-HR-03: a national holiday, excluded from working-day counts. */
@Entity
@Table(name = "public_holidays", schema = "hr")
public class PublicHoliday {

    @Id
    private UUID id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PublicHoliday() {
    }

    public PublicHoliday(LocalDate date, String name) {
        this.id = UUID.randomUUID();
        this.date = date;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
