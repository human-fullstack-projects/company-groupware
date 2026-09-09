package com.company.groupware.service;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(
            String loginId,
            String password,
            String emplName,
            String emplEmail,
            String emplPhone) {

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

        // 비밀번호는 변경하지 않고 나머지 값의 앞뒤 공백 제거
        loginId = loginId.trim();
        emplName = emplName.trim();
        emplEmail = emplEmail.trim();
        emplPhone = emplPhone.trim();

        // 2. Entity에 설정된 최대 길이 확인
        if (loginId.length() > 50
                || emplName.length() > 50
                || emplEmail.length() > 50
                || emplPhone.length() > 50) {
            throw new IllegalArgumentException(
                    "아이디, 이름, 이메일, 전화번호는 각각 50자 이내로 입력해주세요."
            );
        }

        // 현재 사용하는 BCrypt의 비밀번호 길이 제한 확인
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "비밀번호가 너무 깁니다. 영문·숫자는 72자 이내이며, 한글은 더 짧게 입력해주세요."
            );
        }

        // 3. 이메일 기본 형식 확인
        if (!emplEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "올바른 이메일 형식으로 입력해주세요."
            );
        }

        // 4. 아이디 중복 확인
        if (employeeRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        // 5. 입력받은 정보를 직원 Entity에 설정
        Employee employee = new Employee();
        employee.setLoginId(loginId);
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setEmplName(emplName);
        employee.setEmplEmail(emplEmail);
        employee.setEmplPhone(emplPhone);

        // 회원가입으로 관리자 권한을 받을 수 없도록 지정
        employee.setEmplStat(false);

        // 부서·직급은 이후 관리자가 지정

        // 6. DB 저장
        Employee savedEmployee = employeeRepository.save(employee);
        return savedEmployee.getEmplId();
    }
}