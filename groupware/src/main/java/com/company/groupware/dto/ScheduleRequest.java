package com.company.groupware.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

//일정 등록,수정

@Getter
@Setter
@NoArgsConstructor
public class ScheduleRequest {

    private String title;
    private String content;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    // 수정할 때는 조회한 일정의 version을 그대로 전달
    private Long version;
}
