package com.company.groupware.repository;

import com.company.groupware.entity.Board;
import com.company.groupware.entity.Schedule;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 홈 전용 읽기 쿼리. 기존 업무 Repository와 저장 동작을 변경하지 않습니다. */
@Repository
@RequiredArgsConstructor
public class HomeDashboardRepository {
    private final EntityManager entityManager;

    // 현재 입장 이후 메시지만 집계. 본인 메시지와 퇴장/비활성 방은 제외합니다.
    public Map<Long, Long> unreadCounts(Long employeeId) {
        return entityManager.createQuery("""
                select a.chatRoom.roomId, count(m.messageId)
                from ChatRoomAffiliation a, ChatRoomMessage m
                where a.employee.emplId = :employeeId and a.roomOutDate is null
                  and a.chatRoom.roomStat = true and m.chatRoom = a.chatRoom
                  and m.createdAt >= a.roomInDate
                  and m.messageId > coalesce(a.lastReadMessageId, 0)
                  and (m.employee is null or m.employee.emplId <> :employeeId)
                group by a.chatRoom.roomId
                """, Object[].class).setParameter("employeeId", employeeId).getResultList()
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    // 권한을 만족하는 방에서, 현재 입장 이후의 마지막 메시지만 5개 방까지 조회합니다.
    public List<Object[]> recentChats(Long employeeId) {
        return entityManager.createQuery("""
                select r.roomId, r.roomName, m.messageContent, m.createdAt
                from ChatRoomAffiliation a join a.chatRoom r
                left join ChatRoomMessage m on m.messageId = (
                    select max(lastMessage.messageId) from ChatRoomMessage lastMessage
                    where lastMessage.chatRoom = r and lastMessage.createdAt >= a.roomInDate
                )
                where a.employee.emplId = :employeeId and a.roomOutDate is null and r.roomStat = true
                order by coalesce(m.createdAt, a.roomInDate) desc, r.roomId desc
                """, Object[].class).setParameter("employeeId", employeeId).setMaxResults(5).getResultList();
    }

    public long pendingApprovalCount(Long employeeId) {
        return entityManager.createQuery("""
                select count(distinct step.document.documentId) from ApprovalDocumentMember step
                where step.approver.emplId = :employeeId and step.status = 'PENDING'
                  and step.document.status = 'IN_PROGRESS'
                """, Long.class).setParameter("employeeId", employeeId).getSingleResult();
    }

    public long todayScheduleCount(LocalDateTime start, LocalDateTime end) {
        return entityManager.createQuery("""
                select count(s) from Schedule s
                where (s.cancelled = false or s.cancelled is null)
                  and s.startAt < :end and s.endAt > :start
                """, Long.class).setParameter("start", start).setParameter("end", end).getSingleResult();
    }

    public List<Schedule> todaySchedules(LocalDateTime start, LocalDateTime end) {
        return entityManager.createQuery("""
                select s from Schedule s join fetch s.department
                where (s.cancelled = false or s.cancelled is null)
                  and s.startAt < :end and s.endAt > :start
                order by s.startAt, s.scheduleId
                """, Schedule.class).setParameter("start", start).setParameter("end", end)
                .setMaxResults(4).getResultList();
    }

    public List<Schedule> upcomingSchedules(LocalDateTime from) {
        return entityManager.createQuery("""
                select s from Schedule s join fetch s.department
                where (s.cancelled = false or s.cancelled is null) and s.startAt >= :from
                order by s.startAt, s.scheduleId
                """, Schedule.class).setParameter("from", from).setMaxResults(4).getResultList();
    }

    public List<Board> recentNotices(List<Long> readableNoticeIds) {
        if (readableNoticeIds.isEmpty()) return List.of();
        return entityManager.createQuery("""
                select b from Board b join fetch b.boardCategory c
                where c.boardCategoryId in :categoryIds
                  and (b.boardStatus = true or b.boardStatus is null)
                order by b.createdAt desc, b.boardId desc
                """, Board.class).setParameter("categoryIds", readableNoticeIds).setMaxResults(5).getResultList();
    }
}
