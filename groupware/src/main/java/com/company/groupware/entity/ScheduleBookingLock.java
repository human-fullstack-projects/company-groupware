package com.company.groupware.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 일정이 하나도 없을 때도 동시 등록을 순서대로 처리하기 위한 잠금용 행.
@Entity
@Table(name = "calendar_booking_lock")
@Getter
@NoArgsConstructor
public class ScheduleBookingLock {
    @Id
    @Column(name = "lock_id")
    private Integer lockId;
}
