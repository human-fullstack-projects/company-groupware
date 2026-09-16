package com.company.groupware.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 조직도 화면에 전달할 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
public class OrganizationChartResponse {

    // 최상단 노드 (대표이사 등)
    private String title;

    // 중간 노드 리스트 (고문, 총괄임원 등)
    private List<NodeItem> topLevelNodes;

    // 하단 부서 및 하위 팀 구조 리스트
    private List<DepartmentNode> departments;

    // [추가] 부서 상세 조직도용 응답 필드
    private String currentDeptName; // 선택된 부서명
    private List<GradeTierGroup> gradeTierGroups; // 직급별 그룹 리스트 (직급 높은 순)

    @Getter
    @Setter
    @Builder
    public static class NodeItem {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @Builder
    public static class DepartmentNode {
        private Long deptId;
        private String deptName;
        // 하단 팀 리스트 필드(subTeams)는 완전히 제거됨
    }
    // [추가] 직급별 그룹 (한 직급에 속한 직원들을 5개씩 쪼개어 행(Row) 단위로 관리)
    @Getter
    @Setter
    @Builder
    public static class GradeTierGroup {
        private String gradeName; // 직급 이름 (예: 부장, 과장, 사원 등)
        private List<List<EmployeeItem>> rowEmployeeLists; // 한 행에 최대 5명씩 담기는 2차원 리스트
    }

    // [추가] 개별 직원 정보 아이템
    @Getter
    @Setter
    @Builder
    public static class EmployeeItem {
        private Long emplId;
        private String emplName;
        private String gradeName;

        // [추가] 상세 모달창에 보여줄 직원 정보 필드들
        private String deptName;// 부서명
        private String phone;   // 연락처
        private String email;   // 이메일
        private String address; // [추가] 주소 필드

    }
}