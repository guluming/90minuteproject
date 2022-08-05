package me.coldrain.ninetyminute.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ParticipatedTeamMemberResponse {

    private Long matchId;
    private Long teamId;
    private List<TeamMember> teamMemberList;

    @Builder
    public ParticipatedTeamMemberResponse(Long matchId, Long teamId, List<TeamMember> teamMemberList) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.teamMemberList = teamMemberList;
    }

    @Getter
    @NoArgsConstructor
    public static class TeamMember {
        private Long memberId;
        private String memberProfileUrl;
        private String nickName;

        @Builder
        public TeamMember(Long memberId, String memberProfileUrl, String nickName) {
            this.memberId = memberId;
            this.memberProfileUrl = memberProfileUrl;
            this.nickName = nickName;
        }

    }
}