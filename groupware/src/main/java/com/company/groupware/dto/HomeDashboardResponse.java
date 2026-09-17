package com.company.groupware.dto;

import java.time.LocalDate;
import java.util.List;

/** 홈에서 사용하는 값만 전달합니다. 직원 Entity나 비밀번호는 담지 않습니다. */
public record HomeDashboardResponse(
        String employeeName, String departmentName, LocalDate today,
        long unreadCount, long pendingApprovalCount, long todayScheduleCount,
        List<ChatItem> chats, List<ScheduleItem> schedules, boolean upcoming,
        List<NoticeItem> notices, String commuteState, String startTime, String finishTime) {
    public record ChatItem(Long roomId, String roomName, String preview, String timeLabel, long unreadCount) {}
    public record ScheduleItem(Long scheduleId, String title, String departmentName, String timeLabel) {}
    public record NoticeItem(Long boardId, String title, String categoryName, String dateLabel) {}
}
