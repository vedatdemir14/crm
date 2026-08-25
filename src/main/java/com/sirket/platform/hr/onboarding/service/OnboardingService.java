package com.sirket.platform.hr.onboarding.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.security.CurrentUser;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import com.sirket.platform.hr.onboarding.domain.OnboardingTask;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskStatus;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskTemplate;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskType;
import com.sirket.platform.hr.onboarding.dto.OnboardingDtos;
import com.sirket.platform.hr.onboarding.repository.OnboardingTaskRepository;
import com.sirket.platform.hr.onboarding.repository.OnboardingTaskTemplateRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-HR-06 and FR-HR-07: the joining and leaving checklists.
 */
@Service
public class OnboardingService {

    private static final String ROLE_HR_ADMIN = "ROLE_HR_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final OnboardingTaskRepository taskRepository;
    private final OnboardingTaskTemplateRepository templateRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUser currentUser;

    public OnboardingService(OnboardingTaskRepository taskRepository,
            OnboardingTaskTemplateRepository templateRepository,
            EmployeeRepository employeeRepository, CurrentUser currentUser) {
        this.taskRepository = taskRepository;
        this.templateRepository = templateRepository;
        this.employeeRepository = employeeRepository;
        this.currentUser = currentUser;
    }

    // --- templates ---

    @Transactional(readOnly = true)
    public List<OnboardingDtos.TemplateResponse> listTemplates(OnboardingTaskType type) {
        List<OnboardingTaskTemplate> templates = type == null
                ? templateRepository.findAllByOrderByTaskTypeAscDisplayOrderAsc()
                : templateRepository.findByTaskTypeOrderByDisplayOrderAsc(type);
        return templates.stream().map(OnboardingDtos.TemplateResponse::from).toList();
    }

    @Transactional
    public OnboardingDtos.TemplateResponse createTemplate(OnboardingDtos.TemplateRequest request) {
        requireTemplateNameAvailable(request.taskType(), request.name(), null);
        return OnboardingDtos.TemplateResponse.from(templateRepository.save(new OnboardingTaskTemplate(
                request.name(), request.taskType(), request.displayOrder(), request.offsetDays())));
    }

    @Transactional
    public OnboardingDtos.TemplateResponse updateTemplate(UUID id, OnboardingDtos.TemplateRequest request) {
        OnboardingTaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Görev şablonu bulunamadı: " + id));
        requireTemplateNameAvailable(template.getTaskType(), request.name(), id);
        template.update(request.name(), request.displayOrder(), request.offsetDays());
        return OnboardingDtos.TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        OnboardingTaskTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Görev şablonu bulunamadı: " + id));
        // Tasks already created from a template stand on their own, so removing the template does
        // not disturb checklists that are already running.
        templateRepository.delete(template);
    }

    // --- tasks ---

    @Transactional(readOnly = true)
    public List<OnboardingDtos.TaskResponse> search(UUID employeeId, OnboardingTaskType type,
            OnboardingTaskStatus status, UUID assignedTo) {
        return taskRepository.search(employeeId, type, status, assignedTo).stream()
                .map(OnboardingDtos.TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OnboardingDtos.TaskResponse> myTasks(OnboardingTaskStatus status) {
        return taskRepository.search(null, null, status, currentUser.id()).stream()
                .map(OnboardingDtos.TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OnboardingDtos.ChecklistSummary checklist(UUID employeeId, OnboardingTaskType type) {
        Employee employee = requireEmployee(employeeId);
        List<OnboardingTask> tasks = taskRepository.findByEmployeeIdAndTaskType(employeeId, type);
        LocalDate today = LocalDate.now();

        return new OnboardingDtos.ChecklistSummary(
                employee.getId(),
                employee.getFullName(),
                type,
                tasks.size(),
                (int) tasks.stream().filter(task -> !task.isPending()).count(),
                (int) tasks.stream().filter(task -> task.isOverdue(today)).count(),
                tasks.stream().map(OnboardingDtos.TaskResponse::from).toList());
    }

    @Transactional
    public OnboardingDtos.TaskResponse create(OnboardingDtos.TaskRequest request) {
        Employee employee = requireEmployee(request.employeeId());
        OnboardingTask task = new OnboardingTask(employee, request.taskName(), request.taskType(),
                request.dueDate(), request.assignedTo());
        return OnboardingDtos.TaskResponse.from(taskRepository.save(task));
    }

    /**
     * Creates the whole checklist in one go (FR-HR-06, FR-HR-07). Due dates are anchored to the
     * hire date for joining and the termination date for leaving, so a step such as handover can
     * be scheduled before the last day rather than on it.
     * <p>
     * Re-running is safe: an item that already exists for the employee is skipped and reported
     * rather than duplicated, which matters when HR adds a template after a checklist has started.
     */
    @Transactional
    public OnboardingDtos.ApplyTemplateResponse applyTemplate(UUID employeeId, OnboardingTaskType type,
            UUID defaultAssignee) {
        Employee employee = requireEmployee(employeeId);
        LocalDate anchor = anchorDateFor(employee, type);

        List<OnboardingTask> created = new ArrayList<>();
        int skipped = 0;
        for (OnboardingTaskTemplate template : templateRepository.findByTaskTypeOrderByDisplayOrderAsc(type)) {
            if (taskRepository.existsByEmployeeIdAndTaskTypeAndTaskName(employeeId, type, template.getName())) {
                skipped++;
                continue;
            }
            created.add(taskRepository.save(new OnboardingTask(
                    employee, template.getName(), type, template.dueDateFrom(anchor), defaultAssignee)));
        }

        return new OnboardingDtos.ApplyTemplateResponse(type, anchor, created.size(), skipped,
                created.stream().map(OnboardingDtos.TaskResponse::from).toList());
    }

    @Transactional
    public OnboardingDtos.TaskResponse update(UUID id, OnboardingDtos.UpdateTaskRequest request) {
        OnboardingTask task = requireTask(id);
        task.update(request.taskName(), request.dueDate(), request.assignedTo());
        return OnboardingDtos.TaskResponse.from(taskRepository.save(task));
    }

    /**
     * HR may close any item; anyone else only the ones assigned to them. Letting a colleague tick
     * off "access revoked" on someone else's behalf would make the record say something nobody
     * actually verified.
     */
    @Transactional
    public OnboardingDtos.TaskResponse complete(UUID id) {
        OnboardingTask task = requireTask(id);
        if (!currentUser.hasAnyRole(ROLE_HR_ADMIN, ROLE_ADMIN)
                && (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUser.id()))) {
            throw new ApiExceptions.Forbidden(
                    "Görevi yalnızca atanan kişi veya İK yöneticisi tamamlayabilir");
        }
        task.complete(currentUser.id());
        return OnboardingDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public OnboardingDtos.TaskResponse reopen(UUID id) {
        OnboardingTask task = requireTask(id);
        task.reopen();
        return OnboardingDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID id) {
        taskRepository.delete(requireTask(id));
    }

    // --- internals ---

    /**
     * Offboarding is scheduled around the leaving date, so it cannot be laid out before the
     * employee has actually been marked as leaving.
     */
    private LocalDate anchorDateFor(Employee employee, OnboardingTaskType type) {
        if (type == OnboardingTaskType.ONBOARDING) {
            return employee.getHireDate();
        }
        if (employee.getTerminationDate() == null) {
            throw new ApiExceptions.Conflict(
                    "Çıkış görevleri için önce çalışanın ayrılış tarihi girilmelidir");
        }
        return employee.getTerminationDate();
    }

    private void requireTemplateNameAvailable(OnboardingTaskType type, String name, UUID excludedId) {
        templateRepository.findByTypeAndNameIgnoreCase(type, name).ifPresent(existing -> {
            if (!existing.getId().equals(excludedId)) {
                throw new ApiExceptions.Conflict("Bu türde aynı isimde bir şablon zaten var: " + name);
            }
        });
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + id));
    }

    private OnboardingTask requireTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Görev bulunamadı: " + id));
    }
}
