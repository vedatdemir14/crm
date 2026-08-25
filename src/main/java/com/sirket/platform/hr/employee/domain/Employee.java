package com.sirket.platform.hr.employee.domain;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "employees", schema = "hr")
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Employee {

    @Id
    private UUID id;

    /**
     * The linked login, held as a bare id so the HR module keeps no compile-time dependency on the
     * identity module; the database still enforces the foreign key.
     */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String email;

    private String phone;

    /**
     * KVKK-sensitive. Encrypted at rest by the converter, so the column holds ciphertext and this
     * field is ordinary text everywhere in the code (Veri Modeli §6).
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    // Department and the manager are soft-deleted entities, which Hibernate refuses to map lazily.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "position_title")
    private String positionTitle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Employee() {
    }

    public Employee(UUID userId, String firstName, String lastName, String email, String phone, String nationalId,
            LocalDate birthDate, LocalDate hireDate, EmploymentType employmentType, Department department,
            String positionTitle, Employee manager) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
        this.employmentType = employmentType;
        this.department = department;
        this.positionTitle = positionTitle;
        this.manager = manager;
        this.status = EmployeeStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        requireManagerIsNotSelf(manager);
    }

    public void update(UUID userId, String firstName, String lastName, String email, String phone, String nationalId,
            LocalDate birthDate, LocalDate hireDate, EmploymentType employmentType, Department department,
            String positionTitle, Employee manager) {
        requireManagerIsNotSelf(manager);
        requireManagerIsNotOwnSubordinate(manager);
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.nationalId = nationalId;
        this.birthDate = birthDate;
        this.hireDate = hireDate;
        this.employmentType = employmentType;
        this.department = department;
        this.positionTitle = positionTitle;
        this.manager = manager;
        this.updatedAt = Instant.now();
    }

    /**
     * FR-HR-01 asks for employees to be deactivated rather than deleted, so leaving is a status
     * change that keeps the record and its history intact.
     */
    public void terminate(LocalDate terminationDate) {
        if (status == EmployeeStatus.TERMINATED) {
            throw new ApiExceptions.Conflict("Çalışan zaten işten ayrılmış olarak işaretli");
        }
        if (terminationDate == null) {
            throw new ApiExceptions.BadRequest("Ayrılış tarihi zorunludur");
        }
        if (terminationDate.isBefore(hireDate)) {
            throw new ApiExceptions.BadRequest("Ayrılış tarihi işe giriş tarihinden önce olamaz");
        }
        this.status = EmployeeStatus.TERMINATED;
        this.terminationDate = terminationDate;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(EmployeeStatus newStatus) {
        if (newStatus == EmployeeStatus.TERMINATED) {
            throw new ApiExceptions.BadRequest("İşten ayrılış için ayrılış tarihi ile birlikte istek gönderin");
        }
        if (status == EmployeeStatus.TERMINATED) {
            throw new ApiExceptions.Conflict("İşten ayrılmış çalışan yeniden aktif edilemez");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status != EmployeeStatus.TERMINATED;
    }

    private void requireManagerIsNotSelf(Employee candidate) {
        if (candidate != null && candidate.getId().equals(this.id)) {
            throw new ApiExceptions.BadRequest("Çalışan kendi yöneticisi olamaz");
        }
    }

    /**
     * Without this, two employees can be made each other's manager and any walk up the reporting
     * line — an approval chain, an org chart — loops forever.
     */
    private void requireManagerIsNotOwnSubordinate(Employee candidate) {
        for (Employee node = candidate; node != null; node = node.getManager()) {
            if (node.getId().equals(this.id)) {
                throw new ApiExceptions.BadRequest(
                        "Yönetici ataması döngüsel raporlama zinciri oluşturur");
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getNationalId() {
        return nationalId;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public Department getDepartment() {
        return department;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public Employee getManager() {
        return manager;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
