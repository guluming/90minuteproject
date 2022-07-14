package me.coldrain.ninetyminute.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OfferMatchResponse {

    private Long opposingTeamId;
    private String opposingTeamName;
    private Integer opposingTeamPoint;
    private Double winRate;
    private String greeting;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    @Builder
    public OfferMatchResponse(Long opposingTeamId, String opposingTeamName,
                              Integer opposingTeamPoint, Double winRate,
                              String greeting, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.opposingTeamId = opposingTeamId;
        this.opposingTeamName = opposingTeamName;
        this.opposingTeamPoint = opposingTeamPoint;
        this.winRate = winRate;
        this.greeting = greeting;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }
}
