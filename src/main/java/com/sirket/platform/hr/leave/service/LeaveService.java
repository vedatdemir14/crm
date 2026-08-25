package com.sirket.platform.hr.leave.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.common.security.CurrentUser;
import com.sirket.platform.hr.employee.domain.Employee;
import com.sirket.platform.hr.employee.repository.EmployeeRepository;
import com.sirket.platform.hr.leave.domain.LeaveBalance;
import com.sirket.platform.hr.leave.domain.LeaveRequest;
import com.sirket.platform.hr.leave.domain.LeaveStatus;
import com.sirket.platform.hr.leave.domain.LeaveType;
import com.sirket.platform.hr.leave.dto.LeaveDtos;
import com.sirket.platform.hr.leave.repository.LeaveBalanceRepository;
import com.sirket.platform.hr.leave.repository.LeaveRequestRepository;
import com.sirket.platform.hr.leave.repository.LeaveTypeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-HR-02: leave requests, the approval flow and the balance they draw on.
 */
@Service
public class LeaveService {

    private static final String ROLE_HR_ADMIN = "ROLE_HR_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkingDayCalculator workingDayCalculator;
    private final CurrentUser currentUser;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository, LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository, WorkingDayCalculator workingDayCalculator,
            CurrentUser currentUser) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
        this.workingDayCalculator = workingDayCalculator;
        this.currentUser = currentUser;
    }

    // --- employee self-service ---

    @Transactional
    public LeaveDtos.LeaveRequestResponse requestLeaveForCurrentUser(LeaveDtos.LeaveRequestInput input) {
        return LeaveDtos.LeaveRequestResponse.from(create(currentEmployee(), input));
    }

    @Transactional(readOnly = true)
    public Page<LeaveDtos.LeaveRequestResponse> myRequests(Pageable pageable) {
        return leaveRequestRepository
                .findByEmployeeIdOrderByRequestedAtDesc(currentEmployee().getId(), pageable)
                .map(LeaveDtos.LeaveRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LeaveDtos.LeaveBalanceResponse> myBalances(Integer year) {
        return balancesOf(currentEmployee(), year);
    }

    @Transactional
    public void cancelOwnRequest(UUID id) {
        LeaveRequest request = requireExisting(id);
        if (!request.getEmployee().getId().equals(currentEmployee().getId())) {
            // Reported as "not found" so the API does not confirm that someone else's request exists.
            throw new ApiExceptions.NotFound("İzin talebi bulunamadı: " + id);
        }
        request.cancel();
        leaveRequestRepository.save(request);
    }

    // --- HR and manager side ---

    @Transactional(readOnly = true)
    public Page<LeaveDtos.LeaveRequestResponse> search(UUID employeeId, UUID departmentId, LeaveStatus status,
            Pageable pageable) {
        return leaveRequestRepository.search(employeeId, departmentId, status, pageable)
                .map(LeaveDtos.LeaveRequestResponse::from);
    }

    @Transactional
    public LeaveDtos.LeaveRequestResponse createFor(UUID employeeId, LeaveDtos.LeaveRequestInput input) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + employeeId));
        return LeaveDtos.LeaveRequestResponse.from(create(employee, input));
    }

    /**
     * Approving is where the entitlement is actually spent; the balance is only touched here, so a
     * rejected or cancelled request never costs the employee anything.
     */
    @Transactional
    public LeaveDtos.LeaveRequestResponse approve(UUID id, String note) {
        LeaveRequest request = requireExisting(id);
        requireCanDecide(request);

        LeaveBalance balance = balanceFor(request.getEmployee(), request.getLeaveType(),
                request.getStartDate().getYear());
        balance.consume(request.getDaysCount());
        leaveBalanceRepository.save(balance);

        request.approve(currentUser.id(), note);
        return LeaveDtos.LeaveRequestResponse.from(leaveRequestRepository.save(request));
    }

    @Transactional
    public LeaveDtos.LeaveRequestResponse reject(UUID id, String note) {
        LeaveRequest request = requireExisting(id);
        requireCanDecide(request);
        request.reject(currentUser.id(), note);
        return LeaveDtos.LeaveRequestResponse.from(leaveRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<LeaveDtos.LeaveBalanceResponse> balancesOfEmployee(UUID employeeId, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + employeeId));
        return balancesOf(employee, year);
    }

    @Transactional
    public LeaveDtos.LeaveBalanceResponse adjustBalance(UUID employeeId, LeaveDtos.AdjustBalanceRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ApiExceptions.NotFound("Çalışan bulunamadı: " + employeeId));
        LeaveType type = requireLeaveType(request.leaveTypeId());
        LeaveBalance balance = balanceFor(employee, type, request.year());
        balance.adjustTotal(request.totalDays());
        return LeaveDtos.LeaveBalanceResponse.from(leaveBalanceRepository.save(balance));
    }

    @Transactional(readOnly = true)
    public LeaveDtos.WorkingDaysResponse workingDays(LocalDate startDate, LocalDate endDate) {
        return new LeaveDtos.WorkingDaysResponse(startDate, endDate,
                workingDayCalculator.countWorkingDays(startDate, endDate));
    }

    // --- internals ---

    private LeaveRequest create(Employee employee, LeaveDtos.LeaveRequestInput input) {
        if (!employee.isActive()) {
            throw new ApiExceptions.Conflict("İşten ayrılmış çalışan için izin talebi oluşturulamaz");
        }
        LeaveType type = requireLeaveType(input.leaveTypeId());

        List<LeaveRequest> overlaps = leaveRequestRepository.findBlockingOverlaps(
                employee.getId(), input.startDate(), input.endDate());
        if (!overlaps.isEmpty()) {
            throw new ApiExceptions.Conflict(
                    "Bu tarih aralığında bekleyen veya onaylanmış bir izin talebi zaten var");
        }

        int days = workingDayCalculator.countWorkingDays(input.startDate(), input.endDate());
        LeaveRequest request = new LeaveRequest(employee, type, input.startDate(), input.endDate(),
                days, input.reason());
        return leaveRequestRepository.save(request);
    }

    /**
     * A request is decided by the employee's own manager, or by HR. Anyone else — including another
     * manager — gets nothing, since approving another team's leave is not their call.
     */
    private void requireCanDecide(LeaveRequest request) {
        if (currentUser.hasAnyRole(ROLE_HR_ADMIN, ROLE_ADMIN)) {
            return;
        }
        Employee manager = request.getEmployee().getManager();
        if (manager == null || manager.getUserId() == null
                || !manager.getUserId().equals(currentUser.id())) {
            throw new ApiExceptions.Forbidden(
                    "İzin talebini yalnızca çalışanın yöneticisi veya İK yöneticisi sonuçlandırabilir");
        }
    }

    /**
     * Balances are created on first use from the leave type's default entitlement, so HR does not
     * have to seed a row for every employee and year before anyone can take leave.
     */
    private LeaveBalance balanceFor(Employee employee, LeaveType type, int year) {
        return leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                .orElseGet(() -> leaveBalanceRepository.save(
                        new LeaveBalance(employee, type, year, type.getDefaultAnnualDays())));
    }

    private List<LeaveDtos.LeaveBalanceResponse> balancesOf(Employee employee, Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<LeaveBalance> existing = leaveBalanceRepository
                .findByEmployeeIdAndYearOrderByLeaveTypeNameAsc(employee.getId(), targetYear);
        if (!existing.isEmpty()) {
            return existing.stream().map(LeaveDtos.LeaveBalanceResponse::from).toList();
        }
        // Nothing stored yet: report the entitlement the employee would start the year with rather
        // than an empty list, which reads as "no leave available".
        return leaveTypeRepository.findAllByOrderByNameAsc().stream()
                .map(type -> new LeaveDtos.LeaveBalanceResponse(type.getId(), type.getName(), targetYear,
                        type.getDefaultAnnualDays(), 0, type.getDefaultAnnualDays()))
                .toList();
    }

    private Employee currentEmployee() {
        return employeeRepository.findByUserId(currentUser.id())
                .orElseThrow(() -> new ApiExceptions.NotFound(
                        "Kullanıcı hesabınıza bağlı bir çalışan kaydı yok, İK ile iletişime geçin"));
    }

    private LeaveType requireLeaveType(UUID id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("İzin türü bulunamadı: " + id));
    }

    private LeaveRequest requireExisting(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("İzin talebi bulunamadı: " + id));
    }
}
