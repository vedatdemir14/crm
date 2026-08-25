package com.sirket.platform.crm.task.service;

import com.sirket.platform.common.notification.domain.NotificationType;
import com.sirket.platform.common.notification.service.NotificationService;
import com.sirket.platform.crm.task.domain.Task;
import com.sirket.platform.crm.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-CRM-07: produces notifications for open tasks that are approaching or past their due date.
 * <p>
 * The job is idempotent — {@code createIfAbsent} means running it twice in a day does not produce
 * duplicate reminders — so it is safe to re-run manually and safe if the schedule overlaps.
 */
@Component
public class TaskReminderJob {

    private static final Logger log = LoggerFactory.getLogger(TaskReminderJob.class);

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final int dueSoonDays;

    public TaskReminderJob(TaskRepository taskRepository, NotificationService notificationService,
            @Value("${crm.task.due-soon-days:2}") int dueSoonDays) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.dueSoonDays = dueSoonDays;
    }

    @Scheduled(cron = "${crm.task.reminder-cron:0 0 7 * * *}")
    public void runScheduled() {
        int created = run(LocalDate.now());
        log.info("Görev hatırlatma işi tamamlandı, {} yeni bildirim üretildi", created);
    }

    /**
     * @param today the date to evaluate against; a parameter so the behaviour can be exercised
     *              without waiting for the scheduler
     * @return how many notifications were newly created
     */
    @Transactional
    public int run(LocalDate today) {
        List<Task> candidates = taskRepository.findOpenTasksDueOnOrBefore(today.plusDays(dueSoonDays));
        int created = 0;

        for (Task task : candidates) {
            boolean overdue = task.getDueDate().isBefore(today);
            NotificationType type = overdue ? NotificationType.TASK_OVERDUE : NotificationType.TASK_DUE_SOON;
            String title = overdue ? "Görev süresi geçti" : "Görev süresi yaklaşıyor";
            String message = "%s — son tarih: %s".formatted(task.getTitle(), task.getDueDate());

            if (notificationService.createIfAbsent(
                    task.getAssignedTo(), type, title, message, "TASK", task.getId()).isPresent()) {
                created++;
            }
        }
        return created;
    }
}
