package com.sirket.platform.hr.employee.dto;

import com.sirket.platform.hr.employee.domain.Department;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmployeeStatus;
import com.sirket.platform.hr.employee.domain.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    public record EmployeeRequest(
            UUID userId,
            @NotBlank(message = "Ad zorunludur") @Size(max = 100) String firstName,
            @NotBlank(message = "Soyad zorunludur") @Size(max = 100) String lastName,
            @Email(message = "Geçerli bir e-posta adresi giriniz") @Size(max = 255) String email,
            @Size(max = 50) String phone,
            @Size(max = 20) String nationalId,
            LocalDate birthDate,
            @NotNull(message = "İşe giriş tarihi zorunludur") LocalDate hireDate,
            @NotNull(message = "Çalışma şekli zorunludur") EmploymentType employmentType,
            UUID departmentId,
            @Size(max = 150) String positionTitle,
            UUID managerId) {
    }

    public record TerminateRequest(
            @NotNull(message = "Ayrılış tarihi zorunludur") LocalDate terminationDate) {
    }

    public record ChangeStatusRequest(
            @NotNull(message = "Durum zorunludur") EmployeeStatus status) {
    }

    /**
     * {@code nationalId} is masked unless it was explicitly asked for. Personal data should not
     * travel in every routine response (KVKK, NFR-06), and requiring an explicit flag makes the
     * few places that genuinely need it visible in the access log rather than invisible.
     */
    public record EmployeeResponse(
            UUID id,
            UUID userId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String nationalId,
            LocalDate birthDate,
            LocalDate hireDate,
            EmploymentType employmentType,
            UUID departmentId,
            String departmentName,
            String positionTitle,
            UUID managerId,
            String managerName,
            EmployeeStatus status,
            LocalDate terminationDate,
            Instant createdAt,
            Instant updatedAt) {

        public static EmployeeResponse masked(Employee employee) {
            return build(employee, mask(employee.getNationalId()));
        }

        public static EmployeeResponse withNationalId(Employee employee) {
            return build(employee, employee.getNationalId());
        }

        private static EmployeeResponse build(Employee employee, String nationalId) {
            Department department = employee.getDepartment();
            Employee manager = employee.getManager();
            return new EmployeeResponse(
                    employee.getId(),
                    employee.getUserId(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getEmail(),
                    employee.getPhone(),
                    nationalId,
                    employee.getBirthDate(),
                    employee.getHireDate(),
                    employee.getEmploymentType(),
                    department != null ? department.getId() : null,
                    department != null ? department.getName() : null,
                    employee.getPositionTitle(),
                    manager != null ? manager.getId() : null,
                    manager != null ? manager.getFullName() : null,
                    employee.getStatus(),
                    employee.getTerminationDate(),
                    employee.getCreatedAt(),
                    employee.getUpdatedAt());
        }

        /** Keeps the last four digits so a record can still be recognised without exposing it. */
        private static String mask(String nationalId) {
            if (nationalId == null || nationalId.isBlank()) {
                return null;
            }
            if (nationalId.length() <= 4) {
                return "*".repeat(nationalId.length());
            }
            return "*".repeat(nationalId.length() - 4) + nationalId.substring(nationalId.length() - 4);
        }
    }
}
