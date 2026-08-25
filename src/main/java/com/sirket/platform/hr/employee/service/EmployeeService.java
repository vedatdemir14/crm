package com.sirket.platform.hr.employee.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.employee.domain.Department;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.domain.EmployeeStatus;
import com.sirket.platform.hr.employee.dto.EmployeeDtos;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
    }

    /** Lists never carry the national id, masked or otherwise. */
    @Transactional(readOnly = true)
    public Page<EmployeeDtos.EmployeeResponse> search(String name, UUID departmentId, UUID managerId,
            EmployeeStatus status, Pageable pageable) {
        return employeeRepository.search(name, departmentId, managerId, status, pageable)
                .map(EmployeeDtos.EmployeeResponse::masked);
    }

    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeResponse get(UUID id, boolean includeNationalId) {
        Employee employee = requireExisting(id);
        return includeNationalId
                ? EmployeeDtos.EmployeeResponse.withNationalId(employee)
                : EmployeeDtos.EmployeeResponse.masked(employee);
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse create(EmployeeDtos.EmployeeRequest request) {
        requireUserAvailable(request.userId(), null);
        requireHireDateNotInFuture(request.hireDate());

        Employee employee = new Employee(
                request.userId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.nationalId(),
                request.birthDate(),
                request.hireDate(),
                request.employmentType(),
                resolveDepartment(request.departmentId()),
                request.positionTitle(),
                resolveManager(request.managerId()));
        return EmployeeDtos.EmployeeResponse.masked(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse update(UUID id, EmployeeDtos.EmployeeRequest request) {
        Employee employee = requireExisting(id);
        requireUserAvailable(request.userId(), id);
        requireHireDateNotInFuture(request.hireDate());

        employee.update(
                request.userId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.nationalId(),
                request.birthDate(),
                request.hireDate(),
                request.employmentType(),
                resolveDepartment(request.departmentId()),
                request.positionTitle(),
                resolveManager(request.managerId()));
        return EmployeeDtos.EmployeeResponse.masked(employeeRepository.save(employee));
    }

    /**
     * FR-HR-01: leaving is recorded as a status change so the record and its history survive.
     */
    @Transactional
    public EmployeeDtos.EmployeeResponse terminate(UUID id, LocalDate terminationDate) {
        Employee employee = requireExisting(id);
        if (employeeRepository.existsByManagerId(id)) {
            throw new ApiExceptions.Conflict(
                    "Bu çalışana bağlı ekip üyeleri var, önce onları başka bir yöneticiye atayın");
        }
        employee.terminate(terminationDate);
        return EmployeeDtos.EmployeeResponse.masked(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeDtos.EmployeeResponse changeStatus(UUID id, EmployeeStatus status) {
        Employee employee = requireExisting(id);
        employee.changeStatus(status);
        return EmployeeDtos.EmployeeResponse.masked(employeeRepository.save(employee));
    }

    public Employee requireExisting(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + id));
    }

    private Department resolveDepartment(UUID departmentId) {
        return departmentId == null ? null : departmentService.requireExisting(departmentId);
    }

    private Employee resolveManager(UUID managerId) {
        return managerId == null ? null : requireExisting(managerId);
    }

    /**
     * One login belongs to at most one employee. Without this two employee records could share a
     * user, and the self-service portal would have no single answer to "who am I".
     */
    private void requireUserAvailable(UUID userId, UUID excludedEmployeeId) {
        if (userId == null) {
            return;
        }
        employeeRepository.findByUserId(userId).ifPresent(existing -> {
            if (!existing.getId().equals(excludedEmployeeId)) {
                throw new ApiExceptions.Conflict("Bu kullanıcı hesabı başka bir çalışana bağlı");
            }
        });
    }

    private void requireHireDateNotInFuture(LocalDate hireDate) {
        if (hireDate.isAfter(LocalDate.now())) {
            throw new ApiExceptions.BadRequest("İşe giriş tarihi gelecekte olamaz");
        }
    }
}
