package com.company.groupware.service;

import com.company.groupware.dto.EmployeeSearchRequest;
import com.company.groupware.dto.EmployeeSearchResponse;

import com.company.groupware.dto.ChatRoomEmployeeSearchResponse;

import com.company.groupware.entity.Department;
import com.company.groupware.entity.Employee;
import com.company.groupware.entity.Grade;
import com.company.groupware.repository.ChatRoomEmployeeRepository;
import com.company.groupware.repository.DepartmentRepository;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.repository.GradeRepository;
import com.company.groupware.util.EmployeeSortHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(
            String loginId,
            String password,
            String emplName,
            String emplEmail,
            String emplPhone,
            Long departmentId,
            Long gradeId) {

        // 1. 필수 입력 확인
        if (!StringUtils.hasText(loginId)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(emplName)
                || !StringUtils.hasText(emplEmail)
                || !StringUtils.hasText(emplPhone)) {
            throw new IllegalArgumentException(
                    "아이디, 비밀번호, 이름, 이메일, 전화번호를 모두 입력해주세요."
            );
        }

        if (departmentId == null || gradeId == null) {
            throw new IllegalArgumentException(
                    "부서와 직급을 모두 선택해주세요."
            );
        }

        loginId = loginId.trim();
        emplName = emplName.trim();
        emplEmail = emplEmail.trim();
        emplPhone = normalizePhone(emplPhone);

        // 2. 입력 길이 확인
        if (loginId.length() > 50
                || emplName.length() > 50
                || emplEmail.length() > 50
                || emplPhone.length() > 50) {
            throw new IllegalArgumentException(
                    "아이디, 이름, 이메일, 전화번호는 각각 50자 이내로 입력해주세요."
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다. 영문·숫자는 72자 이내이며, 한글은 더 짧게 입력해주세요."
            );
        }

        // 3. 이메일 기본 형식 확인
        validateEmail(emplEmail);

        // 4. 아이디 중복 확인
        if (employeeRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        // 5. 선택한 부서·직급을 DB에서 조회
        Department department = departmentRepository
                .findById(departmentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 부서입니다."
                        )
                );

        Grade grade = gradeRepository
                .findById(gradeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 직급입니다."
                        )
                );

        // 6. 회원 정보 구성
        Employee employee = new Employee();
        employee.setLoginId(loginId);
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setEmplName(emplName);
        employee.setEmplEmail(emplEmail);
        employee.setEmplPhone(emplPhone);
        employee.setDepartment(department);
        employee.setGrade(grade);
        employee.setEmplStat(false);
        employee.setCreatedAt(LocalDateTime.now());

        // 7. 저장
        Employee savedEmployee = employeeRepository.save(employee);
        return savedEmployee.getEmplId();
    }

    @Transactional(readOnly = true)
    public List<EmployeeSearchResponse> searchEmployees(EmployeeSearchRequest request) {
        String emplName = null;
        Long departmentId = null;
        Long gradeId = null;

        if (request != null) {
            departmentId = request.getDepartmentId();
            gradeId = request.getGradeId();

            if (StringUtils.hasText(request.getEmplName())) {
                emplName = request.getEmplName().trim();
            }
        }

        List<Employee> employees =
                employeeRepository.searchEmployees(departmentId, gradeId, emplName);

        return employees.stream()
                // .sorted(EmployeeSortHelper.EMPLOYEE_COMPARATOR)
                .map(EmployeeSearchResponse::from)
                .toList();
    }

    //    @Transactional(readOnly = true)
    //    public List<EmployeeSearchResponse> searchEmployees(Long departmentId, Long gradeId, String emplName) {
    //        return searchEmployees(new EmployeeSearchRequest(departmentId, gradeId, emplName));
    //    }
    //    // 이름으로 직원 검색 (초대 대상 검색용)
    //    public List<ChatRoomEmployeeSearchResponse> searchByName(String emplName) {
    //        if (!StringUtils.hasText(emplName)) {
    //            return List.of();
    //        }
    //        return employeeRepository.findByEmplNameContaining(emplName.trim())
    //                .stream()
    //                .map(ChatRoomEmployeeSearchResponse::new)
    //                .toList();
    //    }

    @Transactional(readOnly = true)
    public long countAdmins() {
        return employeeRepository.countByEmplStatTrue();
    }

    /**
     * 관리자 계정이 하나도 없으면 기본 관리자 계정(root/root1234)을 생성한다.
     * DB를 초기화한 직후에도 최소 한 명은 로그인해서 관리할 수 있도록 하기 위함.
     */
    @Transactional
    public void ensureDefaultAdmin() {
        if (countAdmins() > 0 || employeeRepository.existsByLoginId("root")) {
            return;
        }

        Employee admin = new Employee();
        admin.setLoginId("root");
        admin.setPasswordHash(passwordEncoder.encode("root1234"));
        admin.setEmplName("관리자");
        admin.setEmplStat(true);
        admin.setCreatedAt(LocalDateTime.now());

        employeeRepository.save(admin);
    }

    /**
     * 관리자로 지정
     */
    @Transactional
    public void promoteToAdmin(Long emplId) {
        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직원입니다."
                ));

        employee.setEmplStat(true);
    }

    /**
     * 관리자 해제 (마지막 남은 관리자는 해제 불가)
     */
    @Transactional
    public void demoteFromAdmin(Long emplId) {
        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직원입니다."
                ));

        if (Boolean.TRUE.equals(employee.getEmplStat())
                && employeeRepository.countByEmplStatTrue() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "마지막 남은 관리자는 해제할 수 없습니다."
            );
        }

        employee.setEmplStat(false);
    }

    /**
     * 소속 부서 변경
     */
    @Transactional
    public void changeDepartment(Long emplId, Long deptId) {
        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직원입니다."
                ));

        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 부서입니다."
                ));

        employee.setDepartment(department);
    }

    /**
     * 직급 변경
     */
    @Transactional
    public void changeGrade(Long emplId, Long gradeId) {
        Employee employee = employeeRepository.findById(emplId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직원입니다."
                ));

        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 직급입니다."
                ));

        employee.setGrade(grade);
    }

    /**
     * 마이페이지 - 본인 연락처/주소/이메일 수정
     * 부서·직급·관리자권한은 절대 건드리지 않음
     */
    @Transactional
    public void updateContactInfo(
            String loginId,
            String emplPhone,
            String emplEmail,
            String address) {

        Employee employee = employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        if (!StringUtils.hasText(emplEmail)
                || !StringUtils.hasText(emplPhone)) {
            throw new IllegalArgumentException(
                    "연락처와 이메일을 모두 입력해주세요."
            );
        }

        emplEmail = emplEmail.trim();
        emplPhone = normalizePhone(emplPhone);
        address = StringUtils.hasText(address)
                ? address.trim()
                : null;

        if (emplEmail.length() > 50
                || emplPhone.length() > 50) {
            throw new IllegalArgumentException(
                    "이메일, 전화번호는 각각 50자 이내로 입력해주세요."
            );
        }

        if (address != null && address.length() > 255) {
            throw new IllegalArgumentException(
                    "주소는 255자 이내로 입력해주세요."
            );
        }

        validateEmail(emplEmail);

        employee.setEmplPhone(emplPhone);
        employee.setEmplEmail(emplEmail);
        employee.setAddress(address);
    }

    /**
     * 마이페이지 - 본인 비밀번호 변경
     */
    @Transactional
    public void changePassword(
            String loginId,
            String currentPassword,
            String newPassword,
            String newPasswordConfirm) {

        // 1. 현재 로그인한 직원 찾기
        Employee employee = employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "존재하지 않는 계정입니다."
                ));

        // 2. 입력값이 비어 있는지 확인
        if (!StringUtils.hasText(currentPassword)
                || !StringUtils.hasText(newPassword)
                || !StringUtils.hasText(newPasswordConfirm)) {

            throw new IllegalArgumentException(
                    "비밀번호를 모두 입력해주세요."
            );
        }

        // 3. 현재 비밀번호가 맞는지 확인
        if (!passwordEncoder.matches(
                currentPassword,
                employee.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "현재 비밀번호가 일치하지 않습니다."
            );
        }

        // 4. 새 비밀번호와 새 비밀번호 확인이 같은지 확인
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException(
                    "새 비밀번호가 서로 일치하지 않습니다."
            );
        }

        // 5. 새 비밀번호 최소 길이 확인
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "새 비밀번호는 8자 이상 입력해주세요."
            );
        }

        // 6. 기존 비밀번호와 같은 비밀번호인지 확인
        if (passwordEncoder.matches(
                newPassword,
                employee.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            );
        }

        // 7. BCrypt에서 처리 가능한 비밀번호 길이 확인
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다. 영문·숫자는 72자 이내이며, 한글은 더 짧게 입력해주세요."
            );
        }

        // 8. 새 비밀번호를 암호화해서 저장
        employee.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );
    }

    /**
     * 전화번호의 하이픈/공백을 정리하고
     * 010-XXXX-XXXX 형식으로 맞춘다.
     */
    private String normalizePhone(String rawPhone) {

        String phoneDigits = rawPhone.trim()
                .replace("-", "")
                .replaceAll("\\s+", "");

        if (!phoneDigits.matches("010[0-9]{8}")) {
            throw new IllegalArgumentException(
                    "010으로 시작하는 휴대전화 번호 11자리를 입력해주세요."
            );
        }

        return phoneDigits.substring(0, 3)
                + "-"
                + phoneDigits.substring(3, 7)
                + "-"
                + phoneDigits.substring(7);
    }

    /**
     * 이메일 기본 형식 확인
     */
    private void validateEmail(String email) {

        if (!email.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            throw new IllegalArgumentException(
                    "올바른 이메일 형식으로 입력해주세요."
            );
        }
    }
}