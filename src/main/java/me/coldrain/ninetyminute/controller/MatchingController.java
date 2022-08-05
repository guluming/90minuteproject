package me.coldrain.ninetyminute.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.coldrain.ninetyminute.dto.request.ApprovedMatchRequest;
import me.coldrain.ninetyminute.dto.request.MatchResultRequest;
import me.coldrain.ninetyminute.dto.request.MatchScoreRequest;
import me.coldrain.ninetyminute.dto.request.MatchMemberRequest;
import me.coldrain.ninetyminute.dto.response.*;
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
     * 대결 요청 목록 조회 API
     * 대결 요청 페이지 대결 수락 목록 조회(대결 신청 받은 팀의 팀장만 조회 가능).
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
     * 대결 수락 시 대결 상세 정보 저장 및 apply 상태 변경(대결 요청).
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/teams/{apply_team_id}/apply/{apply_id}")
    public String approveApplyMatch(
            final @PathVariable("apply_team_id") Long applyTeamId,
            final @PathVariable("apply_id") Long applyId,
            final @RequestBody ApprovedMatchRequest approvedMatchRequest,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.approveApplyMatch(applyTeamId, applyId, approvedMatchRequest, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 신청 거절 API
     * 대결 신청 팀의 id을 전달 받아야 함.
     */
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/teams/{apply_team_id}/apply/{apply_id}")
    public void cancelApplyMatch(
            final @PathVariable("apply_team_id") Long applyTeamId,
            final @PathVariable("apply_id") Long applyId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchingService.rejectApplyMatch(applyTeamId, applyId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 소속 팀 목록 조회 API
     * 소속 팀 목록 조회
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/members/{member_id}/matches")         // teamId 가 아니라 memberId
    public List<ParticipationTeamMatchResponse> searchMatches(
            final @PathVariable("member_id") Long memberId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.searchMyTeams(memberId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 예정된 대결 목록 조회 API
     * 성사 된 대결 목록 조회
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/teams/{team_id}/matches")
    public List<MatchResponse> searchApprovedMatches(
            @PathVariable("team_id") Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.searchMatches(teamId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 성사 목록 상세 조회 API
     * 성사 된 대결의 상세 페이지 정보
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/teams/{team_id}/matches/{match_id}/detail")
    public MatchResponse searchApprovedMatchDetail(
            @PathVariable("team_id") Long teamId,
            @PathVariable("match_id") Long matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.searchApprovedMatchDetail(teamId, matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 성사 대결 팀 포매이션 등록 API
     * 비회원의 경우 memberId를 null 처리
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/teams/{team_id}/matches/{match_id}/formation")
    public void makeTeamFormation(
            @PathVariable("team_id") Long teamId,
            @PathVariable("match_id") Long matchId,
            @RequestBody List<MatchMemberRequest> matchMemberRequestList,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchingService.makeTeamFormation(teamId, matchId, matchMemberRequestList, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 등록된 포매이션 조회 API
     * 등록된 포매이션 필드 맴버들을 응답
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/teams/{team_id}/matches/{match_id}/formation")
    public MatchMemberResponse showFormation(
            @PathVariable("team_id") Long teamId,
            @PathVariable("match_id") Long matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.showFormation(teamId, matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 성사 대결 전 취소 API
     * 우천 및 기타 상황 발생 시 대결 취소
     */
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/matches/{match_id}")
    public void cancelApprovedMatch(
            @PathVariable("match_id") Long matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchingService.cancelApprovedMatch(matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 종료 확인 API
     * 대결 종료 확인 버튼 수행 API -> 대결 각 팀의 경기 종료 상태를 apply 에 저장.
     */
    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/matches/{match_id}")
    public void confirmEndMatch(
            final @PathVariable("match_id") Long matchId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchingService.confirmEndMatch(matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 결과 점수 입력 API
     * 대결 후 경기 결과 입력
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/matches/{match_id}/score")
    public void writeMatchScore(
            final @PathVariable("match_id") Long matchId,
            final @RequestBody MatchScoreRequest matchScoreRequest,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        matchingService.writeMatchScore(matchId, matchScoreRequest, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 상대팀 대결 결과 점수 조회 API
     * 상대방이 입력한 점수를 확인 하기 위한 API
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/matches/{match_id}/score")
    public MatchScoreResponse showMatchScore(
            @PathVariable("match_id") Long matchId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.showMatchScore(matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 상대팀 대결 결과 점수 정정 API
     * 대결 점수 정정 API
     */
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/matches/{match_id}/score")
    public MatchScoreResponse correctMatchScore(
            final @PathVariable("match_id") Long matchId,
            final @RequestBody MatchScoreRequest matchScoreRequest,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.correctMatchScore(matchId, matchScoreRequest, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 종료 점수 승인 API
     * API 요청시 admitStatus 값이 true 변경
     */
    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/matches/{match_id}/score/admit")
    public MatchScoreResponse confirmMatchScore(
            final @PathVariable("match_id") Long matchId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return matchingService.confirmMatchScore(matchId, userDetails.getUser());
    }

    /*
     * Author: 병민
     * 대결 후 결과 등록 API
     * 대결 후 결과 등록과 승리 팀 경기에 참여한 선수들에 개별 포지션 점수 및 팀 승점 부여
     * mvp, 분위기 매이커 점수 부여
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/matches/{match_id}/results")
    public void writeMatchResult(
            @PathVariable("match_id") Long matchId,
            @RequestBody MatchResultRequest matchResultRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
//        log.info(matchResultRequest.toString());
        matchingService.writeMatchResult(matchId, matchResultRequest, userDetails.getUser());
    }
}