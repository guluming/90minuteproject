package me.coldrain.ninetyminute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class MyRankResponse {
    private Long memberId;
    private String profileImagerUrl;
    private String nickname;
    private String position;
    private int mvpPoint;
    private int positionPoint;
    private int myRank;
}
