package me.coldrain.ninetyminute.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MyInfoResponse {
    private String nickname;
    private String profileImageUrl;
    private String contact;
    private String phone;
    private String position;
    private int mvpPoint;
    private int totalMyTeamWinCount;
    private int totalMyTeamGameCount;
    private int strikerPoint;
    private int midfielderPoint;
    private int defenderPoint;
    private int goalkeeperPoint;
    private int charmingPoint;
}
