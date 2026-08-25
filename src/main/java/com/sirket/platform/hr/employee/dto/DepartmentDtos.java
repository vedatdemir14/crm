package com.sirket.platform.hr.employee.dto;

import com.sirket.platform.hr.employee.domain.Department;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class DepartmentDtos {

    private DepartmentDtos() {
    }

    public record DepartmentRequest(
            @NotBlank(message = "Departman adı zorunludur") @Size(max = 150) String name,
            UUID parentDepartmentId) {
    }

    public record DepartmentResponse(
            UUID id,
            String name,
            UUID parentDepartmentId,
            String parentDepartmentName) {

        public static DepartmentResponse from(Department department) {
            Department parent = department.getParent();
            return new DepartmentResponse(
                    department.getId(),
                    department.getName(),
                    parent != null ? parent.getId() : null,
                    parent != null ? parent.getName() : null);
        }
    }

    /**
     * FR-HR-10 asks for a department-based org chart, so the tree is returned already nested
     * rather than leaving the client to rebuild it from parent ids.
     */
    public record DepartmentNode(
            UUID id,
            String name,
            long employeeCount,
            List<DepartmentNode> children) {
    }
}
