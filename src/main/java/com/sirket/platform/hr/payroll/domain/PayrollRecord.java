package com.sirket.platform.hr.payroll.domain;

import com.sirket.platform.common.security.crypto.EncryptedBigDecimalConverter;
import com.sirket.platform.hr.employee.domain.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * One payslip for one month. While payroll is mocked every row carries {@code mock = true}, and
 * that flag is surfaced in the API so nothing downstream can mistake a generated figure for a real
 * one (FR-HR-04).
 */
@Entity
@Table(name = "payroll_records", schema = "hr")
public class PayrollRecord {

    @Id
    private UUID id;

    // Employee is a soft-deleted entity, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String period;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "is_mock", nullable = false)
    private boolean mock;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PayrollRecord() {
    }

    public PayrollRecord(Employee employee, YearMonth period, BigDecimal grossAmount, BigDecimal netAmount,
            String currency, boolean mock) {
        this.id = UUID.randomUUID();
        this.employee = employee;
        this.period = period.toString();
        this.grossAmount = grossAmount;
        this.netAmount = netAmount;
        this.currency = currency;
        this.generatedAt = Instant.now();
        this.mock = mock;
        this.createdAt = this.generatedAt;
    }

    public BigDecimal getTotalDeductions() {
        return grossAmount.subtract(netAmount);
    }

    public UUID getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public YearMonth getPeriod() {
        return YearMonth.parse(period);
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public boolean isMock() {
        return mock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
