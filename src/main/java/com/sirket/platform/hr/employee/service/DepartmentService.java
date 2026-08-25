package com.sirket.platform.hr.employee.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.employee.domain.Department;
import com.sirket.platform.hr.employee.dto.DepartmentDtos;
import com.sirket.platform.hr.employee.repository.DepartmentHeadcount;
import com.sirket.platform.hr.employee.repository.DepartmentRepository;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentResponse> list() {
        return departmentRepository.findAllByOrderByNameAsc().stream()
                .map(DepartmentDtos.DepartmentResponse::from)
                .toList();
    }

    /**
     * FR-HR-10: the department hierarchy as a nested tree. Counts are the employees sitting
     * directly in each department, not a rolled-up total, so a manager reading the chart can tell
     * where people actually are rather than seeing every parent inflated by its children.
     */
    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentNode> orgChart() {
        List<Department> departments = departmentRepository.findAllByOrderByNameAsc();
        Map<UUID, Long> headcounts = new HashMap<>();
        for (DepartmentHeadcount row : employeeRepository.headcountByDepartment()) {
            headcounts.put(row.departmentId(), row.employeeCount());
        }

        Map<UUID, List<DepartmentDtos.DepartmentNode>> childrenByParent = new HashMap<>();
        Map<UUID, DepartmentDtos.DepartmentNode> nodes = new LinkedHashMap<>();

        // Built bottom-up would need ordering guarantees; instead each node gets a mutable child
        // list that is filled in as the parents are discovered.
        for (Department department : departments) {
            List<DepartmentDtos.DepartmentNode> children = new ArrayList<>();
            childrenByParent.put(department.getId(), children);
            nodes.put(department.getId(), new DepartmentDtos.DepartmentNode(
                    department.getId(),
                    department.getName(),
                    headcounts.getOrDefault(department.getId(), 0L),
                    children));
        }

        List<DepartmentDtos.DepartmentNode> roots = new ArrayList<>();
        for (Department department : departments) {
            DepartmentDtos.DepartmentNode node = nodes.get(department.getId());
            Department parent = department.getParent();
            if (parent != null && childrenByParent.containsKey(parent.getId())) {
                childrenByParent.get(parent.getId()).add(node);
            }
            else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse create(DepartmentDtos.DepartmentRequest request) {
        requireNameAvailable(request.name(), null);
        Department parent = resolveParent(request.parentDepartmentId());
        return DepartmentDtos.DepartmentResponse.from(
                departmentRepository.save(new Department(request.name(), parent)));
    }

    @Transactional
    public DepartmentDtos.DepartmentResponse update(UUID id, DepartmentDtos.DepartmentRequest request) {
        Department department = requireExisting(id);
        requireNameAvailable(request.name(), id);
        department.update(request.name(), resolveParent(request.parentDepartmentId()));
        return DepartmentDtos.DepartmentResponse.from(departmentRepository.save(department));
    }

    @Transactional
    public void delete(UUID id) {
        Department department = requireExisting(id);
        if (departmentRepository.existsByParentId(id)) {
            throw new ApiExceptions.Conflict("Alt departmanları olan bir departman silinemez");
        }
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ApiExceptions.Conflict("Departmana bağlı çalışanlar var, önce onları taşıyın");
        }
        departmentRepository.delete(department);
    }

    public Department requireExisting(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Departman bulunamadı: " + id));
    }

    private Department resolveParent(UUID parentId) {
        return parentId == null ? null : requireExisting(parentId);
    }

    private void requireNameAvailable(String name, UUID excludedId) {
        departmentRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludedId)) {
                throw new ApiExceptions.Conflict("Bu isimde bir departman zaten var: " + name);
            }
        });
    }
}
