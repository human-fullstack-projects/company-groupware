package com.company.groupware.service;

import com.company.groupware.entity.Commute;
import com.company.groupware.entity.CommuteStatus;
import com.company.groupware.entity.Employee;
import com.company.groupware.repository.CommuteRepository;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommuteBatchService {

    private final EmployeeRepository employeeRepository;
    private final CommuteRepository commuteRepository;


    public void markAbsentees() {

        LocalDate today = LocalDate.now();

        if(isWeekend(today)) {
            System.out.println("[결근 배치] : " + today + " 는 주말이라 결근 판정을 건너 뜁니다.");
            return;
        }

        // 1. 오늘 근무해야 하는 전체 직원
        List<Long> allEmplIds = employeeRepository.findAll().stream().map(Employee::getEmplId).toList();

        // 2. 오늘 이미 출근 기록이 있는 직원
        Set<Long> checkedInEmplIds = new HashSet<>(commuteRepository.findEmplIdsByAttendanceDate(today));

        // 3 . 1-2 -> 결근 대상
        List<Commute> absentees = new ArrayList<>();

        for(Long emplId : allEmplIds) {

            if(checkedInEmplIds.contains(emplId)) {
                continue;
            }

            Employee employee = employeeRepository.getReferenceById(emplId);

            Commute commute = new Commute();
            commute.setEmployee(employee);
            commute.setAttendanceDate(today);
            commute.setStatus(CommuteStatus.ABSENT);
            absentees.add(commute);

        }

        if(absentees.isEmpty()) {
            System.out.println("결근자 없음 : " + today + "결근 처리 " + absentees.size() + "건");

        }

        commuteRepository.saveAll(absentees);
        System.out.println("[결근 배치] " + today + " 결근 처리 " + absentees.size() + "건" + "(empl_id=" + absentees.stream().map(c -> c.getEmployee().getEmplId()).toList());
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
