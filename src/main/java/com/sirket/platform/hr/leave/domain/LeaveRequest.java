package com.sirket.platform.hr.leave.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.employee.domain.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_requests", schema = "hr")
public class LeaveRequest {

    @Id
    private UUID id;

    // Employee and LeaveType are soft-deleted entities, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Working days only: weekends and public holidays are already excluded when this is computed,
     * so the value is what actually comes off the employee's entitlement.
     */
    @Column(name = "days_count", nullable = false)
    private int daysCount;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    /** The user who approved or rejected; null while the request is still pending. */
    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "decision_note")
    private String decisionNote;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeaveRequest() {
    }

    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
            int daysCount, String reason) {
        if (endDate.isBefore(startDate)) {
            throw new ApiExceptions.BadRequest("Bitiş tarihi başlangıç tarihinden önce olamaz");
        }
        if (daysCount <= 0) {
            throw new ApiExceptions.BadRequest(
                    "Seçilen aralıkta iş günü yok; hafta sonu ve resmi tatiller izinden düşülmez");
        }
        this.id = UUID.randomUUID();
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysCount = daysCount;
        this.reason = reason;
        this.status = LeaveStatus.PENDING;
        this.requestedAt = Instant.now();
        this.updatedAt = this.requestedAt;
    }

    public void approve(UUID approverId, String note) {
        requirePending();
        this.status = LeaveStatus.APPROVED;
        this.approverId = approverId;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
        this.updatedAt = this.decidedAt;
    }

    public void reject(UUID approverId, String note) {
        requirePending();
        this.status = LeaveStatus.REJECTED;
        this.approverId = approverId;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
        this.updatedAt = this.decidedAt;
    }

    /**
     * Only a pending request can be withdrawn, matching the API design document. Cancelling an
     * approved leave would have to give the balance back and is left as a separate decision.
     */
    public void cancel() {
        requirePending();
        this.status = LeaveStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public boolean isPending() {
        return status == LeaveStatus.PENDING;
    }

    public boolean overlapsWith(LocalDate otherStart, LocalDate otherEnd) {
        return !startDate.isAfter(otherEnd) && !endDate.isBefore(otherStart);
    }

    private void requirePending() {
        if (status != LeaveStatus.PENDING) {
            throw new ApiExceptions.Conflict("İzin talebi zaten sonuçlanmış: " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getDaysCount() {
        return daysCount;
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
