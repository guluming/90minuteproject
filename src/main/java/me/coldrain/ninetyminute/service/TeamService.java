package me.coldrain.ninetyminute.service;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.dto.TeamListSearch;
import me.coldrain.ninetyminute.dto.TeamListSearchCondition;
import me.coldrain.ninetyminute.dto.request.ApplyRequest;
import me.coldrain.ninetyminute.dto.request.RecruitStartRequest;
import me.coldrain.ninetyminute.dto.request.TeamModifyRequest;
import me.coldrain.ninetyminute.dto.request.TeamRegisterRequest;
import me.coldrain.ninetyminute.entity.*;
import me.coldrain.ninetyminute.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamQueryRepository teamQueryRepository;
    private final WeekdayRepository weekdayRepository;
    private final TimeRepository timeRepository;
    private final RecordRepository recordRepository;
    private final ParticipationRepository participationRepository;
    private final ApplyRepository applyRepository;

    private final MemberRepository memberRepository;
    private final AwsS3Service awsS3Service;

    @Transactional
    public void registerTeam(final TeamRegisterRequest request, final Long memberId) {
        final Map<String, String> uploadFile = awsS3Service.uploadFile(request.getTeamImageFile());
        final String imageFileUrl = uploadFile.get("url");

        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        final Team openTeam = member.getOpenTeam();
        if (openTeam != null) {
            throw new IllegalArgumentException("이미 개설한 팀이 존재 합니다.");
        }

        final Record emptyRecord = recordRepository.save(new Record());
        final Team team = Team.builder()
                .name(request.getTeamName())
                .teamProfileUrl(imageFileUrl)
                .introduce(request.getIntroduce())
                .mainArea(request.getMainArea())
                .preferredArea(request.getPreferredArea())
                .point(0)
                .recruit(false)
                .match(false)
                .record(emptyRecord)
                .build();

        teamRepository.save(team);
        member.setOpenTeam(team);

        if (request.getWeekday() != null) {
            request.getWeekday()
                    .forEach(weekday -> weekdayRepository.save(new Weekday(weekday, team)));
        }

        if (request.getTime() != null) {
            request.getTime()
                    .forEach(time -> timeRepository.save(new Time(time, team)));
        }
    }

    public Page<TeamListSearch> searchTeamList(final TeamListSearchCondition searchCondition, final Pageable pageable) {
        return teamQueryRepository.findAllTeamListSearch(searchCondition, pageable);
    }

    public String findQuestionByTeamId(final Long teamId) {
        return teamRepository.findQuestionByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 팀 질문이 존재하지 않습니다."));
    }

    public Team findById(final Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
    }

    @Transactional
    public void startRecruit(final Long teamId, final Member member, final RecruitStartRequest request) {
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(teamId)) {
            throw new IllegalArgumentException("팀 개설자만 팀원 모집을 할 수 있습니다.");
        }

        final Boolean recruit = team.getRecruit();
        if (recruit.equals(true)) {
            throw new IllegalArgumentException("이미 팀원 모집 중입니다.");
        }

        team.changeRecruit(true);
        team.setQuestion(request.getQuestion());
    }

    @Transactional
    public void endRecruit(final Long teamId, final Member member) {
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(teamId)) {
            throw new IllegalArgumentException("팀 개설자만 팀원 모집 종료를 할 수 있습니다.");
        }

        final Boolean recruit = team.getRecruit();
        if (recruit.equals(false)) {
            throw new IllegalArgumentException("이미 팀원 모집을 종료 상태입니다.");
        }

        team.changeRecruit(false);
        team.setQuestion(null);
    }

    @Transactional
    public void registMatch(final Long teamId, final Member member) {
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(team.getId())) {
            throw new IllegalArgumentException("팀 개설자가 아닙니다.");
        }

        if (team.getMatch().equals(true)) {
            throw new IllegalArgumentException("이미 매칭 등록 상태입니다.");
        }

        team.changeMatch(true);
    }

    @Transactional
    public void cancelMatch(final Long teamId, final Member member) {
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(team.getId())) {
            throw new IllegalArgumentException("팀 개설자가 아닙니다.");
        }

        if (team.getMatch().equals(false)) {
            throw new IllegalArgumentException("이미 매칭 등록 상태가 아닙니다.");
        }

        team.changeMatch(false);
    }

    @Transactional
    public void leaveTeam(final Long teamId, final Member member) {
        final Long openTeamId = member.getOpenTeam().getId();
        if (openTeamId.equals(teamId)) {
            throw new IllegalArgumentException("팀 개설자는 팀을 탈퇴 할 수 없습니다.");
        }

        final Participation participation = participationRepository.findByTeamIdAndMemberId(teamId, member.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 팀에 참여 중이 아닙니다."));

        if (participation.getApproved().equals(false)) {
            throw new IllegalArgumentException("참여 신청 상태에서는 팀 탈퇴를 할 수 없습니다.");
        }

        participationRepository.delete(participation);
    }

    @Transactional
    public void applyMatch(final Long applyTeamId, ApplyRequest applyRequest, final Long teamId) {
        final Team applyTeam = teamRepository.findById(applyTeamId)
                .orElseThrow(() -> new IllegalArgumentException("대결 신청 팀을 찾을 수 없습니다."));
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Apply apply = Apply.builder()
                .applyTeam(applyTeam)
                .team(team)
                .greeting(applyRequest.getGreeting())
                .approved(false)
                .build();

        applyRepository.save(apply);
    }

    @Transactional
    public void cancelApplyMatch(final Long applyTeamId, final Long teamId) {
        final Apply apply = applyRepository.findByApplyTeamIdAndTeamId(applyTeamId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("대결 신청 정보를 찾을 수 없습니다."));

        applyRepository.delete(apply);
    }

    @Transactional
    public void releaseTeamMember(final Long teamId, final Long memberId) {
        try{
            final Participation participation = participationRepository.findByTeamIdAndMemberIdTrue(teamId, memberId);
            participationRepository.delete(participation);
        } catch (Exception e) {
            throw new NullPointerException("해당 회원은 팀원이 아닙니다.");
        }
        
    public void modifyTeam(final Long teamId, final TeamModifyRequest request, final Long id) {
        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        // 기존 파일 삭제
        awsS3Service.deleteFile(team.getTeamProfileUrl());

        // 파일 신규 저장
        final Map<String, String> uploadFile = awsS3Service.uploadFile(request.getTeamImageFile());

        // 기존 weekdays, time 제거
        weekdayRepository.findAllByTeamId(teamId)
                        .forEach(wd -> weekdayRepository.deleteById(wd.getId()));
        timeRepository.findAllByTeamId(teamId)
                        .forEach(t -> timeRepository.deleteById(t.getId()));

        team.modifyTeam(
                uploadFile.get("url"),
                request.getIntroduce(),
                request.getMainArea(),
                request.getPreferredArea(),
                request.getWeekdays(),
                request.getTime()
        );
    }
}
