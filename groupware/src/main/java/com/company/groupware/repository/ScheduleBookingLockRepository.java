package com.company.groupware.repository;

import com.company.groupware.entity.ScheduleBookingLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ScheduleBookingLockRepository
        extends JpaRepository<ScheduleBookingLock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ScheduleBookingLock l where l.lockId = 1")
    Optional<ScheduleBookingLock> lockCalendar();
}
