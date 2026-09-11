package com.company.groupware.util;

import com.company.groupware.entity.Department;
import com.company.groupware.entity.Employee;
import com.company.groupware.entity.Grade;

import java.util.Comparator;
import java.util.List;

/**
 * 직원 목록 정렬 및 부서/직급 우선순위 판별 헬퍼 클래스
 */
public class EmployeeSortHelper {

    /**
     * 부서 기본 정렬 순서:
     * 인사팀 -> 재무팀 -> 기획팀 -> 개발팀 -> 디자인팀
     */
    public static final List<String> DEPARTMENT_ORDER = List.of(
            "인사팀",
            "재무팀",
            "기획팀",
            "개발팀",
            "디자인팀"
    );

    /**
     * "부장" 직급 서열 기준 점수 (200점)
     */
    public static final int BUJANG_RANK = 200;

    /**
     * 직급 서열 점수 계산 (점수가 높을수록 높은 직급)
     */
    public static int getGradeRank(Grade grade) {
        if (grade == null) {
            return -1;
        }
        String name = grade.getGradeName() != null ? grade.getGradeName().trim() : "";
        int namedRank = getGradeRankByName(name);
        if (namedRank != -1) {
            return namedRank;
        }

        // 명칭에 매핑되지 않은 경우 gradeId 기반 점수 산정 (DB: 1:사원 ~ 6:부장)
        if (grade.getGradeId() != null) {
            return (int) (grade.getGradeId() * 30);
        }
        return 0;
    }

    /**
     * 직급명에 따른 서열 점수
     */
    public static int getGradeRankByName(String name) {
        if (name == null || name.isBlank()) {
            return -1;
        }
        return switch (name) {
            case "회장" -> 1000;
            case "부회장" -> 900;
            case "대표이사", "대표" -> 800;
            case "사장" -> 700;
            case "부사장" -> 600;
            case "전무", "전무이사" -> 500;
            case "상무", "상무이사" -> 400;
            case "이사" -> 300;
            case "본부장" -> 250;
            case "부장" -> 200; // 기준점
            case "차장" -> 150;
            case "과장" -> 100;
            case "대리" -> 80;
            case "주임" -> 60;
            case "사원" -> 40;
            case "인턴", "수습" -> 20;
            default -> -1;
        };
    }

    /**
     * 직급이 부장보다 높은지 여부
     */
    public static boolean isAboveBujang(Grade grade) {
        if (grade == null) {
            return false;
        }
        return getGradeRank(grade) > BUJANG_RANK;
    }

    /**
     * 부서 우선순위 순번 반환 (인사팀: 1, 재무팀: 2, 기획팀: 3, 개발팀: 4, 디자인팀: 5, 기타 부서: 100, 미지정: 999)
     */
    public static int getDepartmentOrder(Department dept) {
        if (dept == null || dept.getDeptName() == null || dept.getDeptName().isBlank()) {
            return 999;
        }
        String deptName = dept.getDeptName().trim();
        int idx = DEPARTMENT_ORDER.indexOf(deptName);
        if (idx != -1) {
            return idx + 1; // 1 ~ 5
        }
        return 100; // 기타 부서
    }

    /**
     * 직원의 정렬 1차 그룹(Bucket) 번호 결정
     * 1: 부서 미지정 & 부장보다 높은 직급 (가장 위)
     * 2: 인사팀
     * 3: 재무팀
     * 4: 기획팀
     * 5: 개발팀
     * 6: 디자인팀
     * 7: 기타 부서
     * 8: 부서 미지정 & 부장 이하 직급 (디자인팀 이후)
     * 9: 부서 미지정 & 직급 미지정 (디자인팀 이후)
     */
    public static int getEmployeeGroup(Employee employee) {
        if (employee == null) {
            return 999;
        }
        Department dept = employee.getDepartment();
        Grade grade = employee.getGrade();

        boolean hasDept = dept != null && dept.getDeptName() != null && !dept.getDeptName().isBlank();
        boolean hasGrade = grade != null;

        if (hasDept) {
            int deptOrder = getDepartmentOrder(dept);
            if (deptOrder >= 1 && deptOrder <= 5) {
                return deptOrder + 1; // 2(인사), 3(재무), 4(기획), 5(개발), 6(디자인)
            }
            return 7; // 기타 부서
        } else {
            // 부서 미지정인 경우
            if (hasGrade) {
                if (isAboveBujang(grade)) {
                    return 1; // 부장보다 높은 직급 -> 최상단
                } else {
                    return 8; // 부장 이하 직급 -> 디자인팀 이후
                }
            } else {
                return 9; // 부서 및 직급 모두 미지정 -> 디자인팀 이후
            }
        }
    }

    /**
     * 모든 요구사항을 만족하는 직원 정렬 Comparator
     */
    public static final Comparator<Employee> EMPLOYEE_COMPARATOR = (e1, e2) -> {
        // 1. 대분류 그룹 비교 (1 ~ 9)
        int g1 = getEmployeeGroup(e1);
        int g2 = getEmployeeGroup(e2);
        if (g1 != g2) {
            return Integer.compare(g1, g2);
        }

        // 2. 기타 부서(Group 7)인 경우 부서명 가나다순 우선
        if (g1 == 7) {
            String dName1 = e1.getDepartment() != null ? e1.getDepartment().getDeptName() : "";
            String dName2 = e2.getDepartment() != null ? e2.getDepartment().getDeptName() : "";
            int deptComp = dName1.compareTo(dName2);
            if (deptComp != 0) {
                return deptComp;
            }
        }

        // 3. 부서가 지정된 그룹(Group 2 ~ 7):
        // 부서는 지정되었으나 직급이 없는 직원은 해당 부서의 마지막에 나옴
        if (g1 >= 2 && g1 <= 7) {
            boolean hasGrade1 = e1.getGrade() != null;
            boolean hasGrade2 = e2.getGrade() != null;
            if (hasGrade1 != hasGrade2) {
                return hasGrade1 ? -1 : 1; // 직급 있는 직원이 앞, 직급 없는 직원이 뒤
            }
        }

        // 4. 직급 순위 비교 (높은 직급부터 내림차순)
        Grade grade1 = e1.getGrade();
        Grade grade2 = e2.getGrade();
        int rank1 = getGradeRank(grade1);
        int rank2 = getGradeRank(grade2);
        if (rank1 != rank2) {
            return Integer.compare(rank2, rank1); // 내림차순
        }

        // 5. 이름 기준 오름차순 (가나다순)
        String name1 = e1.getEmplName() != null ? e1.getEmplName() : "";
        String name2 = e2.getEmplName() != null ? e2.getEmplName() : "";
        int nameComp = name1.compareTo(name2);
        if (nameComp != 0) {
            return nameComp;
        }

        // 6. 식별자(ID) 기준 오름차순 (안정적 정렬)
        Long id1 = e1.getEmplId() != null ? e1.getEmplId() : 0L;
        Long id2 = e2.getEmplId() != null ? e2.getEmplId() : 0L;
        return Long.compare(id1, id2);
    };
}
