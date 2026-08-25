package com.sirket.platform.hr.employee.repository;

import java.util.UUID;

/** Number of employees sitting directly in a department, used to build the org chart. */
public record DepartmentHeadcount(UUID departmentId, long employeeCount) {
}
