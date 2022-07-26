package me.coldrain.ninetyminute.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.coldrain.ninetyminute.dto.TeamListSearch;
import me.coldrain.ninetyminute.dto.TeamListSearchCondition;
import me.coldrain.ninetyminute.dto.request.*;
import me.coldrain.ninetyminute.dto.response.ApplyTeamResponse;
import me.coldrain.ninetyminute.dto.response.TeamImageRegisterResponse;
import me.coldrain.ninetyminute.dto.response.TeamParticipationQuestionResponse;
import me.coldrain.ninetyminute.security.UserDetailsImpl;
import me.coldrain.ninetyminute.service.ParticipationService;
import me.coldrain.ninetyminute.service.TeamService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TeamController {

    private final TeamService teamService;
    private final ParticipationService participationService;

    /**
     * Author: 상운
     * 팀 등록 API
     * 한 명당 하나의 팀만 개설할 수 있습니다.
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/home/teams")
    public void registerTeam(
            final @RequestBody TeamRegisterRequest request,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("registerTeam.TeamRegisterRequest = {}", request);

        teamService.registerTeam(request, userDetails.getUser().getId());
    }

    /**
     * Author: 상운
     * 팀 이미지 파일 등록 API
     * S3에 저장 후 URL을 응답한다.
     */
    @PostMapping("/home/teams/image")
    public TeamImageRegisterResponse registerTeamImage(final MultipartFile teamImageFile) {

        log.info("registerImage.teamImageFile = {}", teamImageFile);
        final String s3uploadFileUrl = teamService.registerTeamImage(teamImageFile);
        return new TeamImageRegisterResponse(s3uploadFileUrl);
    }

    /**
     * Author: 범수
     * 팀 정보 조회 API
     */
    @GetMapping("/home/teams/{team_id}")
    public ResponseEntity<?> infoTeam(
            @PathVariable("team_id") Long teamId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return teamService.infoTeam(teamId, userDetails);
    }

    /**
     * Author: 상운
     * 팀 목록 조회 API
     */
    @GetMapping("/home/teams")
    public Slice<TeamListSearch> selectTeams(
            final TeamListSearchCondition searchCondition,
            final Pageable pageable) {

        log.info("selectTeams.searchCondition = {}", searchCondition);
        return teamService.searchTeamList(searchCondition, pageable);
    }

    /**
     * Author: 상운
     * 팀 참여 질문 조회 API
     */
    @GetMapping("/teams/{team_id}/questions")
    public TeamParticipationQuestionResponse getQuestion(
            final @PathVariable("team_id") Long teamId) {

        final String question = teamService.findQuestionByTeamId(teamId);
        return new TeamParticipationQuestionResponse(question);
    }

    /**
     * Author: 상운
     * 팀 참여 신청 API
     * 한 번 참여 신청한 팀은 다시 참여 신청을 할 수 없습니다.
     */
    @PostMapping("/home/teams/{team_id}/participate")
    public void participate(
            final @PathVariable("team_id") Long teamId,
            final @RequestBody TeamParticipateRequest request,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        participationService.participate(teamId, userDetails.getUser(), request);
    }

    /**
     * Author: 상운
     * 신청한 팀원 승인 API
     * 팀 개설자만 승인을 할 수 있습니다.
     */
    @PatchMapping("/teams/{team_id}/members/{member_id}/offer")
    public void approve(
            final @PathVariable("team_id") Long teamId,
            final @PathVariable("member_id") Long memberId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        participationService.approve(teamId, memberId, userDetails.getUser());
    }

    /**
     * Author: 상운
     * 신청한 팀원 거절 API
     * 팀 개설자만 거절을 할 수 있습니다.
     */
    @DeleteMapping("/teams/{team_id}/members/{member_id}/offer")
    public void disapprove(
            final @PathVariable("team_id") Long teamId,
            final @PathVariable("member_id") Long memberId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        participationService.disapprove(teamId, memberId, userDetails.getUser());
    }

    /**
     * Author: 상운
     * 팀원 모집 시작 API
     */
    @PostMapping("/home/teams/{team_id}/recruit/start")
    public void startRecruit(
            final @PathVariable("team_id") Long teamId,
            final @RequestBody RecruitStartRequest request,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.startRecruit(teamId, userDetails.getUser(), request);
    }

    /**
     * Author: 상운
     * 팀원 모집 종료 API
     */
    @PostMapping("/home/teams/{team_id}/recruit/end")
    public void endRecruit(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.endRecruit(teamId, userDetails.getUser());
    }

    /**
     * Author: 상운
     * 대결 등록 API
     */
    @PostMapping("/home/teams/{team_id}/match/regist")
    public void registerMatch(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.registerMatch(teamId, userDetails.getUser());
    }

    /**
     * Author: 상운
     * 대결 등록 취소 API
     */
    @PostMapping("/home/teams/{team_id}/match/cancel")
    public void cancelMatch(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.cancelMatch(teamId, userDetails.getUser());
    }

    /**
     * Author: 상운
     * 팀 탈퇴 API
     */
    @DeleteMapping("/home/teams/{team_id}/leave")
    public void leaveTeam(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.leaveTeam(teamId, userDetails.getUser());
    }

    /**
     * Author: 상운, 병민
     * 대결 신청 API
     */
    @PostMapping("/home/teams/{team_id}/match/apply")
    public void applyMatch(
            final @PathVariable("team_id") Long teamId,
            final @RequestBody ApplyRequest applyRequest,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        final Long applyTeamId = userDetails.getUser()
                .getOpenTeam()
                .getId();

        teamService.applyMatch(applyTeamId, applyRequest, teamId);
    }

    /**
     * Author: 상운
     * 대결 신청 취소 API
     */
    @PostMapping("/home/teams/{team_id}/match/apply/cancel")
    public void cancelApplyMatch(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        final Long applyTeamId = userDetails.getUser()
                .getOpenTeam()
                .getId();

        teamService.cancelApplyMatch(applyTeamId, teamId);
    }

    /**
     * Author: 범수
     * 팀원 추방 API
     */
    @DeleteMapping("/teams/{team_id}/members/{member_id}/participation")
    public ResponseEntity<?> releaseTeamMember(
            final @PathVariable("team_id") Long teamId,
            final @PathVariable("member_id") Long memberId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return teamService.releaseTeamMember(teamId, memberId, userDetails);
    }

     /**
     * Author: 상운
     * 팀 수정 API
     */
    @PatchMapping("/home/teams/{team_id}")
    public void modifyTeam(
            final @PathVariable("team_id") Long teamId,
            final @RequestBody TeamModifyRequest request,
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {

        teamService.modifyTeam(teamId, request, userDetails.getUser().getId());
    }

    /*
     * Author: 병민
     * 팀 참여 신청한 팀 목록 조회 API
     * 신청한 팀 목록을 조회한다.
     */
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/teams/apply")
    public List<ApplyTeamResponse> searchApplyTeams(
            final @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return teamService.searchApplyTeams(userDetails.getUser());
    }

    /*
     * Author: 병민
     * 팀 해체 API
     * 자신이 개설한 팀의 해체
     */
    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/home/teams/{team_id}/disband")
    public void disbandTeam(
            final @PathVariable("team_id") Long teamId,
            final @AuthenticationPrincipal UserDetailsImpl userDetails ) {
        teamService.disbandTeam(teamId, userDetails.getUser());
    }
}