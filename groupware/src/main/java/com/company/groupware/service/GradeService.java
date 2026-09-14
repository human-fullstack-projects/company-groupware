package com.company.groupware.service;

import com.company.groupware.entity.Grade;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final EmployeeRepository employeeRepository;

    // DB에 등록된 전체 직급 목록을 직급 중요도(숫자가 작을수록 높은 서열) 순으로 정렬하여 반환
    @Transactional(readOnly = true)
    public List<Grade> getGrades() {
        List<Grade> list = gradeRepository.findAll();
        return list.stream()
                .sorted(Comparator.comparing(Grade::getGradePriority, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Grade::getGradeName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * 직급 일괄 추가 - 입력한 순서대로 현재 목록 맨 아래(가장 낮은 서열)에 이어 붙임
     */
    @Transactional
    public void addGrades(List<String> gradeNames) {
        if (gradeNames == null || gradeNames.isEmpty()) {
            throw new IllegalArgumentException("추가할 직급 이름을 입력해주세요.");
        }

        int nextPriority = gradeRepository.findAll().stream()
                .map(Grade::getGradePriority)
                .filter(p -> p != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (String rawName : gradeNames) {
            if (!StringUtils.hasText(rawName)) {
                continue;
            }
            String name = rawName.trim();

            if (gradeRepository.existsByGradeName(name)) {
                throw new IllegalArgumentException("이미 존재하는 직급 이름입니다: " + name);
            }

            gradeRepository.save(Grade.builder().gradeName(name).gradePriority(nextPriority).build());
            nextPriority++;
        }
    }

    /**
     * 직급 중요도 변경 - 사이 순위에 있던 직급들을 밀어내서 항상 1~N이 겹치지 않게 유지
     */
    @Transactional
    public void updatePriority(Long gradeId, int newPriority) {
        List<Grade> allGrades = gradeRepository.findAll();

        if (newPriority < 1 || newPriority > allGrades.size()) {
            throw new IllegalArgumentException(
                    "순위는 1~" + allGrades.size() + " 사이여야 합니다."
            );
        }

        Grade target = allGrades.stream()
                .filter(g -> g.getGradeId().equals(gradeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직급입니다."
                ));

        Integer oldPriority = target.getGradePriority();
        if (oldPriority == null || oldPriority.equals(newPriority)) {
            target.setGradePriority(newPriority);
            return;
        }

        for (Grade grade : allGrades) {
            if (grade.getGradeId().equals(gradeId) || grade.getGradePriority() == null) {
                continue;
            }
            if (newPriority < oldPriority
                    && grade.getGradePriority() >= newPriority
                    && grade.getGradePriority() < oldPriority) {
                grade.setGradePriority(grade.getGradePriority() + 1);
            } else if (newPriority > oldPriority
                    && grade.getGradePriority() > oldPriority
                    && grade.getGradePriority() <= newPriority) {
                grade.setGradePriority(grade.getGradePriority() - 1);
            }
        }

        target.setGradePriority(newPriority);
    }

    /**
     * 직급 삭제 (소속 직원이 있으면 삭제 불가) - 삭제된 순위보다 아래였던 직급들은 한 칸씩 당겨서 빈 자리 없앰
     */
    @Transactional
    public void deleteGrade(Long gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직급입니다."
                ));

        if (employeeRepository.countByGrade_GradeId(gradeId) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "이 직급에 소속된 직원이 있어 삭제할 수 없습니다."
            );
        }

        Integer deletedPriority = grade.getGradePriority();
        gradeRepository.delete(grade);

        if (deletedPriority != null) {
            for (Grade remaining : gradeRepository.findAll()) {
                if (remaining.getGradePriority() != null && remaining.getGradePriority() > deletedPriority) {
                    remaining.setGradePriority(remaining.getGradePriority() - 1);
                }
            }
        }
    }
}