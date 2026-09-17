package com.company.groupware.service;

import com.company.groupware.dto.HomeDashboardResponse;
import com.company.groupware.dto.HomeDashboardResponse.*;
import com.company.groupware.entity.*;
import com.company.groupware.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeDashboardService {
    private final EmployeeRepository employeeRepository;
    private final HomeDashboardRepository dashboardRepository;
    private final BoardService boardService;
    private final CommuteRepository commuteRepository;

    public HomeDashboardResponse getDashboard(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Employee employee = employeeRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "직원 정보를 찾을 수 없습니다."));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime start = today.atStartOfDay(), end = today.plusDays(1).atStartOfDay();

        var unread = dashboardRepository.unreadCounts(employee.getEmplId());
        List<ChatItem> chats = dashboardRepository.recentChats(employee.getEmplId()).stream()
                .map(r -> new ChatItem((Long) r[0], text((String) r[1], "이름 없는 채팅방"),
                        preview((String) r[2]),
                        chatTime((LocalDateTime) r[3], today), unread.getOrDefault((Long) r[0], 0L)))
                .toList();

        long todayCount = dashboardRepository.todayScheduleCount(start, end);
        boolean upcoming = todayCount == 0;
        List<Schedule> scheduleEntities = upcoming ? dashboardRepository.upcomingSchedules(end)
                : dashboardRepository.todaySchedules(start, end);
        List<ScheduleItem> schedules = scheduleEntities.stream()
                .map(s -> new ScheduleItem(s.getScheduleId(), s.getTitle(), s.getDepartment().getDeptName(),
                        scheduleTime(s, today, upcoming))).toList();

        // 게시판의 기존 권한 함수를 재사용합니다. 제목만 보여도 읽기 권한이 필요합니다.
        List<Long> noticeIds = boardService.visibleCategories(employee).stream()
                .filter(c -> c.getBoardCategoryName() != null && c.getBoardCategoryName().endsWith("공지"))
                .map(BoardCategory::getBoardCategoryId).toList();
        List<NoticeItem> notices = dashboardRepository.recentNotices(noticeIds).stream()
                .map(b -> new NoticeItem(b.getBoardId(), b.getBoardTitle(), b.getBoardCategory().getBoardCategoryName(),
                        b.getCreatedAt() == null ? "" : b.getCreatedAt().format(DateTimeFormatter.ofPattern("MM.dd"))))
                .toList();

        Commute commute = commuteRepository.findByEmployee_EmplIdAndAttendanceDate(employee.getEmplId(), today).orElse(null);
        String state = commute == null ? "출근 전"
                : commute.getStatus() == CommuteStatus.ABSENT ? "결근"
                : hasText(commute.getFinishTime()) ? "퇴근 완료"
                : hasText(commute.getStartTime()) ? (commute.getStatus() == CommuteStatus.LATE ? "근무 중 · 지각" : "근무 중") : "출근 전";

        return new HomeDashboardResponse(employee.getEmplName(), employee.getDepartment() == null ? "부서 미배정" : employee.getDepartment().getDeptName(),
                today, unread.values().stream().mapToLong(Long::longValue).sum(),
                dashboardRepository.pendingApprovalCount(employee.getEmplId()), todayCount,
                chats, schedules, upcoming, notices, state,
                commute == null ? "—" : text(commute.getStartTime(), "—"),
                commute == null ? "—" : text(commute.getFinishTime(), "—"));
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String text(String value, String fallback) { return hasText(value) ? value : fallback; }
    private static String preview(String value) {
        String content = text(value, "메시지가 없거나 첨부파일 메시지입니다.");
        int count = content.codePointCount(0, content.length());
        return count > 110 ? content.substring(0, content.offsetByCodePoints(0, 110)) + "…" : content;
    }
    private static String chatTime(LocalDateTime time, LocalDate today) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern(time.toLocalDate().equals(today) ? "HH:mm" : "MM.dd"));
    }
    private static String scheduleTime(Schedule s, LocalDate today, boolean upcoming) {
        boolean sameDay = s.getStartAt().toLocalDate().equals(today) && s.getEndAt().toLocalDate().equals(today);
        var format = DateTimeFormatter.ofPattern(!upcoming && sameDay ? "HH:mm" : "MM.dd HH:mm");
        return s.getStartAt().format(format) + " – " + s.getEndAt().format(format);
    }
}
