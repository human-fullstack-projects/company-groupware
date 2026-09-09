package com.company.groupware.service;

import com.company.groupware.entity.Grade;
import com.company.groupware.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    // DB에 등록된 전체 직급 목록 조회
    @Transactional(readOnly = true)
    public List<Grade> getGrades() {
        return gradeRepository.findAll();
    }
}