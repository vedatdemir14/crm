package com.sirket.platform.hr.payroll.dto;

import com.sirket.platform.hr.payroll.domain.PayrollRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PayrollDtos {

    private PayrollDtos() {
    }

    /**
     * {@code mock} is part of the contract, not a debugging aid: while payroll is stubbed, every
     * consumer needs to be able to tell that these amounts were generated (FR-HR-04).
     */
    public record PayrollRecordResponse(
            UUID id,
            UUID employeeId,
            String employeeName,
            String period,
            BigDecimal grossAmount,
            BigDecimal netAmount,
            BigDecimal totalDeductions,
            String currency,
            Instant generatedAt,
            boolean mock) {

        public static PayrollRecordResponse from(PayrollRecord record) {
            return new PayrollRecordResponse(
                    record.getId(),
                    record.getEmployee().getId(),
                    record.getEmployee().getFullName(),
                    record.getPeriod().toString(),
                    record.getGrossAmount(),
                    record.getNetAmount(),
                    record.getTotalDeductions(),
                    record.getCurrency(),
                    record.getGeneratedAt(),
                    record.isMock());
        }
    }
}
