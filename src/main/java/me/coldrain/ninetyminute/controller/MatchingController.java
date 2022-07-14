package me.coldrain.ninetyminute.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.coldrain.ninetyminute.dto.request.ApprovedMatchRequest;
import me.coldrain.ninetyminute.dto.response.OfferMatchResponse;
import me.coldrain.ninetyminute.security.UserDetailsImpl;
import me.coldrain.ninetyminute.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MatchingController {

    private final MatchingService matchingService;

    /*
     * Author: 병민
     * 대결 수락 목록 조회 API
     * 대결이 신청된 목록을 대결 신청 받은 팀의 팀장이 조회하는 API.
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/teams/{team_id}/matches/offer")
    public List<OfferMatchResponse> searchOfferMatches(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        return matchingService.searchOfferMatches(teamId, userDetails.getUser());
    }


    /*
     * Author: 병민
     * 대결 수락 정보 저장 API
     * 대결 수락 시 대결 상세 정보 저장 및 apply 상태 변경.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/teams/{apply_team_id}/apply")
    public String searchApprovedMatch(
            final @PathVariable Long apply_team_id,
            final @RequestBody ApprovedMatchRequest approvedMatchRequest,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        return matchingService.approvedMatch(apply_team_id, approvedMatchRequest, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 신청 거절 API
     * 대결 신청 팀의 id을 전달 받아야 함.
     */
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/teams/{apply_team_id}/apply")
    public void cancelApplyMatch(
            final @PathVariable Long apply_team_id,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        matchingService.rejectApplyMatch(apply_team_id, userDetails.getUser().getOpenTeam().getId());
    }
}
