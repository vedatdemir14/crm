package com.sirket.platform.hr.onboarding.repository;

import com.sirket.platform.hr.onboarding.domain.OnboardingTaskTemplate;
import com.sirket.platform.hr.onboarding.domain.OnboardingTaskType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingTaskTemplateRepository extends JpaRepository<OnboardingTaskTemplate, UUID> {

    List<OnboardingTaskTemplate> findByTaskTypeOrderByDisplayOrderAsc(OnboardingTaskType taskType);

    List<OnboardingTaskTemplate> findAllByOrderByTaskTypeAscDisplayOrderAsc();

    @Query("""
            SELECT t FROM OnboardingTaskTemplate t
            WHERE t.taskType = :taskType AND LOWER(t.name) = LOWER(CAST(:name AS String))
            """)
    Optional<OnboardingTaskTemplate> findByTypeAndNameIgnoreCase(
            @Param("taskType") OnboardingTaskType taskType, @Param("name") String name);
}
