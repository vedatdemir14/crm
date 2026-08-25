package com.sirket.platform.hr.leave.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.employee.domain.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An employee's entitlement for one leave type in one year. Kept per year because entitlements
 * reset annually and last year's usage must stay readable.
 */
@Entity
@Table(name = "leave_balances", schema = "hr")
public class LeaveBalance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(name = "total_days", nullable = false)
    private int totalDays;

    @Column(name = "used_days", nullable = false)
    private int usedDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LeaveBalance() {
    }

    public LeaveBalance(Employee employee, LeaveType leaveType, int year, int totalDays) {
        this.id = UUID.randomUUID();
        this.employee = employee;
        this.leaveType = leaveType;
        this.year = year;
        this.totalDays = totalDays;
        this.usedDays = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public int getRemainingDays() {
        return totalDays - usedDays;
    }

    /**
     * Deducting is refused rather than allowed to go negative: an approval that silently overdraws
     * the entitlement is the kind of thing nobody notices until payroll.
     */
    public void consume(int days) {
        if (days > getRemainingDays()) {
            throw new ApiExceptions.Conflict(
                    "Yetersiz izin bakiyesi: kalan %d gün, talep edilen %d gün"
                            .formatted(getRemainingDays(), days));
        }
        this.usedDays += days;
        this.updatedAt = Instant.now();
    }

    public void restore(int days) {
        this.usedDays = Math.max(0, this.usedDays - days);
        this.updatedAt = Instant.now();
    }

    public void adjustTotal(int totalDays) {
        if (totalDays < usedDays) {
            throw new ApiExceptions.BadRequest(
                    "Toplam gün, kullanılmış %d günden az olamaz".formatted(usedDays));
        }
        this.totalDays = totalDays;
        this.updatedAt = Instant.now();
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

    public int getYear() {
        return year;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public int getUsedDays() {
        return usedDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
