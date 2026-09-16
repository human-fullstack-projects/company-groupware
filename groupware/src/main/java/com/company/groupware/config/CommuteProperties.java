package com.company.groupware.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
/**
 * application.properties 의 app.commute.late-time 값을 바인딩합니다.
 * 값을 지정하지 않으면 기본값 09:00 이 사용됩니다.
 *
 * 예) application.properties
 *   app.commute.late-time=09:00:00
 */
@Component
@ConfigurationProperties(prefix = "app.commute")
@Getter
@Setter
public class CommuteProperties {
    private LocalTime lateTime = LocalTime.of(9,0); // 지각 기준 시각 기본 09시
}
