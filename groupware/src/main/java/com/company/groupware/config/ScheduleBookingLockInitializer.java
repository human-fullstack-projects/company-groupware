package com.company.groupware.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 일정 중복 검사에 필요한 공통 잠금 행을 준비합니다.
 * 테이블 생성은 기존 JPA 스키마 설정이 담당합니다.
 */
@Component
public class ScheduleBookingLockInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ScheduleBookingLockInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        // MariaDB용: 여러 서버가 동시에 시작해도 PK 중복으로 실패하지 않습니다.
        // 기존 행은 삭제하거나 교체하지 않고 같은 ID를 유지합니다.
        jdbcTemplate.update("""
                INSERT INTO calendar_booking_lock (lock_id)
                VALUES (1)
                ON DUPLICATE KEY UPDATE lock_id = 1
                """);
    }
}
