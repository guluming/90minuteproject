package me.coldrain.ninetyminute.dto.request;

import lombok.*;

import java.time.LocalTime;
import java.util.Date;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
public class ApprovedMatchRequest {

    private Date matchDate;
    private String meridiem;
    private LocalTime time;
    private String matchLocation;
}