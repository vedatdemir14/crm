package com.sirket.platform.hr.leave.dto;

import com.sirket.platform.hr.leave.domain.LeaveBalance;
import com.sirket.platform.hr.leave.domain.LeaveRequest;
import com.sirket.platform.hr.leave.domain.LeaveStatus;
import com.sirket.platform.hr.leave.domain.LeaveType;
import com.sirket.platform.hr.leave.domain.PublicHoliday;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class LeaveDtos {

    private LeaveDtos() {
    }

    // --- leave types (FR-HR-03) ---

    public record LeaveTypeRequest(
            @NotBlank(message = "İzin türü adı zorunludur") @Size(max = 100) String name,
            boolean paid,
            @PositiveOrZero(message = "Varsayılan gün sayısı negatif olamaz") int defaultAnnualDays) {
    }

    public record LeaveTypeResponse(UUID id, String name, boolean paid, int defaultAnnualDays) {

        public static LeaveTypeResponse from(LeaveType type) {
            return new LeaveTypeResponse(type.getId(), type.getName(), type.isPaid(),
                    type.getDefaultAnnualDays());
        }
    }

    // --- public holidays (FR-HR-03) ---

    public record PublicHolidayRequest(
            @NotNull(message = "Tarih zorunludur") LocalDate date,
            @NotBlank(message = "Tatil adı zorunludur") @Size(max = 150) String name) {
    }

    public record PublicHolidayResponse(UUID id, LocalDate date, String name) {

        public static PublicHolidayResponse from(PublicHoliday holiday) {
            return new PublicHolidayResponse(holiday.getId(), holiday.getDate(), holiday.getName());
        }
    }

    // --- leave requests (FR-HR-02) ---

    public record LeaveRequestInput(
            @NotNull(message = "İzin türü zorunludur") UUID leaveTypeId,
            @NotNull(message = "Başlangıç tarihi zorunludur") LocalDate startDate,
            @NotNull(message = "Bitiş tarihi zorunludur") LocalDate endDate,
            @Size(max = 1000) String reason) {
    }

    public record DecisionRequest(@Size(max = 1000) String note) {
    }

    public record LeaveRequestResponse(
            UUID id,
            UUID employeeId,
            String employeeName,
            UUID leaveTypeId,
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            int daysCount,
            String reason,
            LeaveStatus status,
            UUID approverId,
            String decisionNote,
            Instant requestedAt,
            Instant decidedAt) {

        public static LeaveRequestResponse from(LeaveRequest request) {
            return new LeaveRequestResponse(
                    request.getId(),
                    request.getEmployee().getId(),
                    request.getEmployee().getFullName(),
                    request.getLeaveType().getId(),
                    request.getLeaveType().getName(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getDaysCount(),
                    request.getReason(),
                    request.getStatus(),
                    request.getApproverId(),
                    request.getDecisionNote(),
                    request.getRequestedAt(),
                    request.getDecidedAt());
        }
    }

    // --- balances ---

    public record LeaveBalanceResponse(
            UUID leaveTypeId,
            String leaveTypeName,
            int year,
            int totalDays,
            int usedDays,
            int remainingDays) {

        public static LeaveBalanceResponse from(LeaveBalance balance) {
            return new LeaveBalanceResponse(
                    balance.getLeaveType().getId(),
                    balance.getLeaveType().getName(),
                    balance.getYear(),
                    balance.getTotalDays(),
                    balance.getUsedDays(),
                    balance.getRemainingDays());
        }
    }

    public record AdjustBalanceRequest(
            @NotNull(message = "İzin türü zorunludur") UUID leaveTypeId,
            @NotNull(message = "Yıl zorunludur") Integer year,
            @PositiveOrZero(message = "Toplam gün negatif olamaz") int totalDays) {
    }

    /**
     * Returned before a request is submitted so the employee can see what the range will actually
     * cost once weekends and holidays are taken out.
     */
    public record WorkingDaysResponse(LocalDate startDate, LocalDate endDate, int workingDays) {
    }
}
