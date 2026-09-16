package com.company.groupware.dto;

import com.company.groupware.entity.CommuteStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommuteUpdateRequest {
    private String startTime;    // HH:mm 또는 HH:mm:ss
    private CommuteStatus status; // NORMAL / LATE / ABSENT
    private String reason;       // 수정 사유
}
