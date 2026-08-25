package com.sirket.platform.crm.task.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.crm.access.CrmAccessPolicy;
import com.sirket.platform.crm.contact.domain.Contact;
import com.sirket.platform.crm.contact.service.ContactService;
import com.sirket.platform.crm.opportunity.domain.Opportunity;
import com.sirket.platform.crm.opportunity.service.OpportunityService;
import com.sirket.platform.crm.task.domain.Task;
import com.sirket.platform.crm.task.domain.TaskStatus;
import com.sirket.platform.crm.task.dto.TaskDtos;
import com.sirket.platform.crm.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    /** Stands in for "no upper bound", so the query never receives a null date. */
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final TaskRepository taskRepository;
    private final ContactService contactService;
    private final OpportunityService opportunityService;
    private final CrmAccessPolicy accessPolicy;

    public TaskService(TaskRepository taskRepository, ContactService contactService,
            OpportunityService opportunityService, CrmAccessPolicy accessPolicy) {
        this.taskRepository = taskRepository;
        this.contactService = contactService;
        this.opportunityService = opportunityService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public Page<TaskDtos.TaskResponse> search(UUID assignedTo, TaskStatus status, LocalDate dueBefore,
            Pageable pageable) {
        return taskRepository
                .search(assignedTo, assigneeRestriction(), status,
                        dueBefore != null ? dueBefore : MAX_DATE, pageable)
                .map(TaskDtos.TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskDtos.TaskResponse get(UUID id) {
        return TaskDtos.TaskResponse.from(requireVisible(id));
    }

    @Transactional
    public TaskDtos.TaskResponse create(TaskDtos.TaskRequest request) {
        requireMayAssignTo(request.assignedTo());
        Task task = new Task(
                request.title(),
                request.description(),
                request.dueDate(),
                request.assignedTo(),
                resolveContact(request.relatedContactId()),
                resolveOpportunity(request.relatedOpportunityId()),
                accessPolicy.currentUserId());
        return TaskDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskDtos.TaskResponse update(UUID id, TaskDtos.TaskRequest request) {
        Task task = requireVisible(id);
        requireMayAssignTo(request.assignedTo());
        task.update(
                request.title(),
                request.description(),
                request.dueDate(),
                request.assignedTo(),
                resolveContact(request.relatedContactId()),
                resolveOpportunity(request.relatedOpportunityId()));
        return TaskDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskDtos.TaskResponse complete(UUID id) {
        Task task = requireVisible(id);
        task.complete();
        return TaskDtos.TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID id) {
        taskRepository.delete(requireVisible(id));
    }

    /**
     * A sales rep may only put work on their own list; handing tasks to other people is a manager's
     * call. Without this a rep could assign work to a colleague and then lose sight of it, since
     * task visibility follows the assignee.
     */
    private void requireMayAssignTo(UUID assignee) {
        if (!accessPolicy.canSeeAllRecords() && !assignee.equals(accessPolicy.currentUserId())) {
            throw new ApiExceptions.Forbidden("Yalnızca satış müdürü başka bir kullanıcıya görev atayabilir");
        }
    }

    private UUID assigneeRestriction() {
        return accessPolicy.canSeeAllRecords() ? null : accessPolicy.currentUserId();
    }

    private Task requireVisible(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Görev bulunamadı: " + id));
        if (!accessPolicy.canSeeAllRecords() && !task.getAssignedTo().equals(accessPolicy.currentUserId())) {
            // Reported as "not found" so the API does not confirm that someone else's task exists.
            throw new ApiExceptions.NotFound("Görev bulunamadı: " + id);
        }
        return task;
    }

    private Contact resolveContact(UUID contactId) {
        return contactId == null ? null : contactService.requireVisible(contactId);
    }

    private Opportunity resolveOpportunity(UUID opportunityId) {
        return opportunityId == null ? null : opportunityService.requireVisible(opportunityId);
    }
}
