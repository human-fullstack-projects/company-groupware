package com.company.groupware.service;

import com.company.groupware.entity.Grade;
import com.company.groupware.repository.GradeRepository;
import com.company.groupware.util.EmployeeSortHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // DB에 등록된 전체 직급 목록을 높은 직급부터 내림차순으로 정렬하여 반환
    @Transactional(readOnly = true)
    public List<Grade> getGrades() {
        List<Grade> list = gradeRepository.findAll();
        return list.stream()
                .sorted(Comparator.comparingInt(EmployeeSortHelper::getGradeRank).reversed()
                        .thenComparing(Grade::getGradeName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}