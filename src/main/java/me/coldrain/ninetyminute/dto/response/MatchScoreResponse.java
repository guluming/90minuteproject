package me.coldrain.ninetyminute.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchScoreResponse {

    private Long matchId;
    private Long teamId;
    private Long opponentTeamId;
    private Integer teamScore;
    private Integer opponentTeamScore;

    @Builder
    public MatchScoreResponse(Long matchId, Long teamId, Long opponentTeamId, Integer teamScore, Integer opponentTeamScore) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.opponentTeamId = opponentTeamId;
        this.teamScore = teamScore;
        this.opponentTeamScore = opponentTeamScore;
    }
}
