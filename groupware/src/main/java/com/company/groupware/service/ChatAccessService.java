package com.company.groupware.service;

import com.company.groupware.entity.Employee;
import com.company.groupware.repository.EmployeeRepository;
import com.company.groupware.repository.ChatRoomAffiliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatAccessService {
    private final EmployeeRepository employeeRepository;
    private final ChatRoomAffiliationRepository affiliationRepository;

    // HTTP의 로그인 Principal 전용. STOMP Principal은 현재 사번이므로 혼용하지 않습니다.
    public Employee getHttpEmployee(Principal principal) {
        if (principal == null || principal instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return employeeRepository.findByLoginId(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "로그인한 직원 정보를 찾을 수 없습니다."));
    }

    public void requireActiveMember(Long emplId, Long roomId) {
        boolean allowed = emplId != null && roomId != null
                && affiliationRepository.findByEmployee_EmplIdAndChatRoom_RoomId(emplId, roomId)
                        .filter(a -> a.getRoomOutDate() == null).isPresent();
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "현재 참여 중인 채팅방에서만 사용할 수 있습니다.");
        }
    }

    // 관리자 전용 채팅 기능 접근 제어 (Employee.emplStat 이 관리자 여부)
    public void requireAdmin(Employee employee) {
        if (employee == null || !Boolean.TRUE.equals(employee.getEmplStat())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 사용할 수 있습니다.");
        }
    }
}
