package com.sirket.platform.hr.leave.service;

import com.sirket.platform.common.error.ApiExceptions;
import com.sirket.platform.hr.leave.domain.LeaveType;
import com.sirket.platform.hr.leave.domain.PublicHoliday;
import com.sirket.platform.hr.leave.dto.LeaveDtos;
import com.sirket.platform.hr.leave.repository.LeaveTypeRepository;
import com.sirket.platform.hr.leave.repository.PublicHolidayRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-HR-03: configurable leave types and the public holiday calendar. */
@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final PublicHolidayRepository holidayRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository, PublicHolidayRepository holidayRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveDtos.LeaveTypeResponse> listTypes() {
        return leaveTypeRepository.findAllByOrderByNameAsc().stream()
                .map(LeaveDtos.LeaveTypeResponse::from)
                .toList();
    }

    @Transactional
    public LeaveDtos.LeaveTypeResponse createType(LeaveDtos.LeaveTypeRequest request) {
        requireTypeNameAvailable(request.name(), null);
        return LeaveDtos.LeaveTypeResponse.from(leaveTypeRepository.save(
                new LeaveType(request.name(), request.paid(), request.defaultAnnualDays())));
    }

    @Transactional
    public LeaveDtos.LeaveTypeResponse updateType(UUID id, LeaveDtos.LeaveTypeRequest request) {
        requireTypeNameAvailable(request.name(), id);
        LeaveType type = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("İzin türü bulunamadı: " + id));
        type.update(request.name(), request.paid(), request.defaultAnnualDays());
        return LeaveDtos.LeaveTypeResponse.from(leaveTypeRepository.save(type));
    }

    @Transactional(readOnly = true)
    public List<LeaveDtos.PublicHolidayResponse> listHolidays(Integer year) {
        List<PublicHoliday> holidays = year == null
                ? holidayRepository.findAllByOrderByDateAsc()
                : holidayRepository.findByDateBetweenOrderByDateAsc(
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return holidays.stream().map(LeaveDtos.PublicHolidayResponse::from).toList();
    }

    @Transactional
    public LeaveDtos.PublicHolidayResponse addHoliday(LeaveDtos.PublicHolidayRequest request) {
        holidayRepository.findByDate(request.date()).ifPresent(existing -> {
            throw new ApiExceptions.Conflict(
                    "Bu tarih zaten resmi tatil olarak tanımlı: " + existing.getName());
        });
        return LeaveDtos.PublicHolidayResponse.from(
                holidayRepository.save(new PublicHoliday(request.date(), request.name())));
    }

    @Transactional
    public void deleteHoliday(UUID id) {
        PublicHoliday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFound("Resmi tatil bulunamadı: " + id));
        holidayRepository.delete(holiday);
    }

    private void requireTypeNameAvailable(String name, UUID excludedId) {
        leaveTypeRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludedId)) {
                throw new ApiExceptions.Conflict("Bu isimde bir izin türü zaten var: " + name);
            }
        });
    }
}
