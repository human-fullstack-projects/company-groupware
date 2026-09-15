package com.company.groupware.scheduler;

import com.company.groupware.service.CommuteBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommuteScheduler {

    private final CommuteBatchService commuteBatchService;

    // cron "초 분 시 일 월 요일"
    @Scheduled(cron = "0 50 23 * * *")
    public void runAbsenteeCheck() {
        commuteBatchService.markAbsentees();
    }

}
