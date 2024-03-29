package me.coldrain.ninetyminute.service;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.dto.TeamListSearch;
import me.coldrain.ninetyminute.dto.TeamListSearchCondition;
import me.coldrain.ninetyminute.dto.request.*;
import me.coldrain.ninetyminute.dto.response.ApplyTeamResponse;
import me.coldrain.ninetyminute.dto.response.ParticipatedTeamMemberResponse;
import me.coldrain.ninetyminute.dto.response.TeamDuplicateResponse;
import me.coldrain.ninetyminute.dto.response.TeamInfoResponse;
import me.coldrain.ninetyminute.entity.*;
import me.coldrain.ninetyminute.repository.*;
import me.coldrain.ninetyminute.security.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final HistoryRepository historyRepository;
    private final BeforeMatchingRepository beforeMatchingRepository;
    private final MemberRepository memberRepository;
    private final AwsS3Service awsS3Service;

    @Transactional
    public void registerTeam(final TeamRegisterRequest request, final Long memberId) {
        final Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        final Team openTeam = member.getOpenTeam();
        if (openTeam != null) {
            throw new IllegalArgumentException("이미 개설한 팀이 존재 합니다.");
        }
//        if (openTeam.getDeleted()) { throw new IllegalArgumentException("해당 팀은 해체 되었습니다."); }
        if (teamRepository.findByTeamName(request.getTeamName()).orElse(null) != null) {
            throw new IllegalArgumentException("중복된 팀 이름 입니다.");
        }

        final Record emptyRecord = recordRepository.save(new Record());
        final Team team = Team.builder()
                .name(request.getTeamName())
                .teamProfileUrl(request.getUrl() != null ? request.getUrl() : null)
                .introduce(request.getIntroduce())
                .mainArea(request.getMainArea())
                .preferredArea(request.getPreferredArea())
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

        final Participation participation = Participation.builder()
                .member(member)
                .team(team)
                .approved(true)
                .build();

        participationRepository.save(participation);
    }

    public ResponseEntity<?> infoTeam(Long teamId, UserDetailsImpl userDetails) {
        Optional<Team> found = teamRepository.findByIdAndDeletedFalse(teamId);
        if (found.isEmpty()) {
            return new ResponseEntity<>("존재하지 않는 팀입니다.", HttpStatus.BAD_REQUEST);
        }

        Team infoTeam = teamRepository.findByIdAndDeletedFalse(teamId).orElseThrow();

        List<String> teamWeekDays = new ArrayList<>();
        List<Weekday> teamWeekdayList = weekdayRepository.findAllByTeamId(teamId);
        for (Weekday weekday : teamWeekdayList) {
            teamWeekDays.add(weekday.getWeekday());
        }

        List<String> teamTimes = new ArrayList<>();
        List<Time> teamTimeList = timeRepository.findAllByTeamId(teamId);
        for (Time time : teamTimeList) {
            teamTimes.add(time.getTime());
        }

        int headCount = participationRepository.findAllByTeamIdTrue(teamId).size();

        boolean teamCaptain = false;
        boolean otherCaptain = false;
        boolean approved = false;
        boolean participate = false;
        boolean matching = false;
        boolean apply = false;

        if (userDetails.getUser().getOpenTeam() != null) {
            if (teamId.equals(userDetails.getUser().getOpenTeam().getId())) {
                teamCaptain = true;
            } else {
                otherCaptain = true;
                Optional<Apply> matchingCheck = applyRepository.findByApplyTeamIdAndTeamIdTrue(userDetails.getUser().getOpenTeam().getId(), teamId);
                if (matchingCheck.isPresent()) {
                    matching = true;
                    apply = true;
                }

                Optional<Apply> applyCheck = applyRepository.findByApplyTeamIdAndTeamId(userDetails.getUser().getOpenTeam().getId(), teamId);
                if (applyCheck.isPresent()) {
                    apply = true;
                }
            }
        }

        Optional<Participation> approvedTeamMember = participationRepository.findByMemberIdAndTeamIdTrue(userDetails.getUser().getId(), teamId);
        if (approvedTeamMember.isPresent()) {
            approved = true;
        }

        Optional<Participation> participateTeam = participationRepository.findByTeamIdAndMemberId(teamId, userDetails.getUser().getId());
        if (participateTeam.isPresent()) {
            participate = true;
        }

        TeamInfoResponse.RecentMatchHistory recentMatchHistory = new TeamInfoResponse.RecentMatchHistory();

        List<BeforeMatching> recentTeamBeforeMatchingList = beforeMatchingRepository.findByRecentTeamBeforeMatching(teamId);
        List<BeforeMatching> recentOpposingTeamBeforeMatchingList = beforeMatchingRepository.findByRecentOpposingTeamBeforeMatching(teamId);

        if (recentTeamBeforeMatchingList.size()!=0 || recentOpposingTeamBeforeMatchingList.size()!=0) {
            BeforeMatching recentBeforeMatching;

            if (recentTeamBeforeMatchingList.size() == 0) {
                recentBeforeMatching = recentOpposingTeamBeforeMatchingList.get(0);
            } else if (recentOpposingTeamBeforeMatchingList.size() == 0) {
                recentBeforeMatching = recentTeamBeforeMatchingList.get(0);
            } else if (recentTeamBeforeMatchingList.get(0).getMatchDate().after(recentOpposingTeamBeforeMatchingList.get(0).getMatchDate())) {
                recentBeforeMatching = recentTeamBeforeMatchingList.get(0);
            } else {
                recentBeforeMatching = recentOpposingTeamBeforeMatchingList.get(0);
            }

            History recentHistory = historyRepository.findByRecentHistory(recentBeforeMatching.getId()).orElseThrow();

            TeamInfoResponse.RecentMatchHistory.Team recentMatchHistoryTeam = new TeamInfoResponse.RecentMatchHistory.Team(
                    recentHistory.getBeforeMatching().getTeamName(),
                    recentHistory.getAfterMatching().getResult(),
                    recentHistory.getAfterMatching().getScore()
            );

            TeamInfoResponse.RecentMatchHistory.OpposingTeam recentMatchHistoryOpposingTeam = new TeamInfoResponse.RecentMatchHistory.OpposingTeam(
                    recentHistory.getBeforeMatching().getOpposingTeamName(),
                    recentHistory.getAfterMatching().getOpponentResult(),
                    recentHistory.getAfterMatching().getOpponentScore()
            );

            recentMatchHistory.setHistoryId(recentHistory.getId());
            recentMatchHistory.setMatchDate(recentBeforeMatching.calculatedDate());
            recentMatchHistory.setTeam(recentMatchHistoryTeam);
            recentMatchHistory.setOpposingTeam(recentMatchHistoryOpposingTeam);
        } else {
            recentMatchHistory = null;
        }

        TeamInfoResponse teamInfoResponse = new TeamInfoResponse(
                infoTeam.getId(),
                infoTeam.getName(),
                infoTeam.getIntroduce(),
                infoTeam.getTeamProfileUrl(),
                infoTeam.getRecruit(),
                infoTeam.getMatches(),
                infoTeam.getRecord().getWinPoint(),
                infoTeam.getRecord().getTotalGameCount(),
                infoTeam.getRecord().getWinCount(),
                infoTeam.getRecord().getDrawCount(),
                infoTeam.getRecord().getLoseCount(),
                infoTeam.getRecord().getWinRate(),
                infoTeam.getMainArea(),
                infoTeam.getPreferredArea(),
                teamWeekDays,
                teamTimes,
                headCount,
                teamCaptain,
                otherCaptain,
                approved,
                participate,
                matching,
                apply,
                recentMatchHistory,
                infoTeam.getCreatedDate(),
                infoTeam.getModifiedDate()
        );
        return new ResponseEntity<>(teamInfoResponse, HttpStatus.OK);
    }

    public Slice<TeamListSearch> searchTeamList(final TeamListSearchCondition searchCondition, final Pageable pageable) {
        return teamQueryRepository.findAllTeamListSearch(searchCondition, pageable);
    }

    public String findQuestionByTeamId(final Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("등록된 팀 질문이 존재하지 않습니다."));
        if (team.getDeleted()) throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        return team.getQuestion();
    }

    public Team findById(final Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
    }

    @Transactional
    public void startRecruit(final Long teamId, final Member member, final RecruitStartRequest request) {
        final Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        if (member.getOpenTeam() == null) {
            throw new IllegalArgumentException("개설된 팀이 없습니다.");
        }
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
        final Team team = teamRepository.findByIdAndDeletedFalse(teamId)
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
    public void registerMatch(final Long teamId, final Member member) {
        final Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(team.getId())) {
            throw new IllegalArgumentException("팀 개설자가 아닙니다.");
        }

        if (team.getMatches().equals(true)) {
            throw new IllegalArgumentException("이미 매칭 등록 상태입니다.");
        }

        team.changeMatch(true);
    }

    @Transactional
    public void cancelMatch(final Long teamId, final Member member) {
        final Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(team.getId())) {
            throw new IllegalArgumentException("팀 개설자가 아닙니다.");
        }

        if (team.getMatches().equals(false)) {
            throw new IllegalArgumentException("이미 매칭 등록 상태가 아닙니다.");
        }

        team.changeMatch(false);
    }

    @Transactional
    public void leaveTeam(final Long teamId, final Member member) {
        if (teamRepository.findByIdAndDeletedTrue(teamId).orElse(null) != null)
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
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
        final Team team = teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
        if (team.getMatches()) {
            if (!applyTeamId.equals(teamId)) {
                final Team applyTeam = teamRepository.findByIdAndDeletedFalse(applyTeamId)
                        .orElseThrow(() -> new IllegalArgumentException("대결 신청 팀을 찾을 수 없습니다."));
                if (applyRepository.findByApplyTeamIdAndTeamId(applyTeamId, teamId).orElse(null) == null
                        || applyRepository.findByApplyTeamIdAndTeamIdAndEndMatch(applyTeamId, teamId).orElse(null) != null) {
                    final Apply apply = Apply.builder()
                            .applyTeam(applyTeam)
                            .team(team)
                            .greeting(applyRequest.getGreeting())
                            .approved(false)
                            .build();

                    applyRepository.save(apply);
                } else throw new IllegalArgumentException("이미 대결 신청한 팀입니다.");
            } else throw new IllegalArgumentException("같은 팀에게 대결을 신청할 수 없습니다.");
        } else throw new IllegalArgumentException("현재 이 팀은 대결 모집 상태가 아닙니다.");
    }

    @Transactional
    public void cancelApplyMatch(final Long applyTeamId, final Long teamId) {
        final Apply apply = applyRepository.findByApplyTeamIdAndTeamId(applyTeamId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("대결 신청 정보를 찾을 수 없습니다."));

        applyRepository.delete(apply);
    }

    @Transactional
    public ResponseEntity<?> releaseTeamMember(final Long teamId,
                                               final Long memberId,
                                               final UserDetailsImpl userDetails) {
        if(teamRepository.findByIdAndDeletedTrue(teamId).orElse(null) != null)
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        Long loginMemberOpenTeamId;
        if (userDetails.getUser().getOpenTeam() != null) {
            loginMemberOpenTeamId = userDetails.getUser().getOpenTeam().getId();
        } else {
            return new ResponseEntity<>("개설한 팀이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (userDetails.getUser().getId().equals(memberId)) {
            return new ResponseEntity<>("자기 자신은 추방할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (loginMemberOpenTeamId.equals(teamId)) {
            final Participation participation = participationRepository.findByMemberIdAndTeamIdTrue(memberId, teamId)
                    .orElseThrow(() -> new IllegalArgumentException("참여에 존재하지 않습니다."));
            participationRepository.delete(participation);
            return new ResponseEntity<>("추방이 완료되었습니다.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("해당 팀의 개설자가 아닙니다.", HttpStatus.BAD_REQUEST);
        }
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

    public List<ApplyTeamResponse> searchApplyTeams(Member member) {
        List<Participation> participationList = participationRepository.findAllByMemberIdApprovedFalse(member.getId());
        List<ApplyTeamResponse> applyTeamResponseList = new ArrayList<>();
        for (Participation participation : participationList) {
            Integer teamMemberCnt = participationRepository.findAllByTeamIdTrue(participation.getTeam().getId()).size();
            ApplyTeamResponse applyTeamResponse = ApplyTeamResponse.builder()
                    .teamId(participation.getTeam().getId())
                    .isCaptain(false)
                    .teamName(participation.getTeam().getName())
                    .teamMemberCount(teamMemberCnt)
                    .teamPoint(participation.getTeam().getRecord().getWinPoint())
                    .teamWinRate(participation.getTeam().getRecord().getWinRate())
                    .teamTotalGameCount(participation.getTeam().getRecord().getTotalGameCount())
                    .teamWinCount(participation.getTeam().getRecord().getWinCount())
                    .teamDrawCount(participation.getTeam().getRecord().getDrawCount())
                    .teamLoseCount(participation.getTeam().getRecord().getLoseCount())
                    .mainArea(participation.getTeam().getMainArea())
                    .createdDate(participation.getCreatedDate())
                    .modifiedDate(participation.getModifiedDate())
                    .applyStatus(false)
                    .build();

            if (participation.getTeam().getId().equals(member.getOpenTeam().getId()))
                applyTeamResponse.changeIsCaptain(true);
            if (participation.getApproved()) applyTeamResponse.changeApplyStatus(true);
            applyTeamResponseList.add(applyTeamResponse);
        }
        return applyTeamResponseList;
    }

    @Transactional
    public ParticipatedTeamMemberResponse disbandTeam(Long teamId, Member member) {
        if(teamRepository.findByIdAndDeletedTrue(teamId).orElse(null) != null)
            throw new IllegalArgumentException("팀을 찾을 수 없습니다.");
        if (member.getOpenTeam() == null) throw new IllegalArgumentException("개설한 팀이 존재하지 않습니다.");
        if (member.getOpenTeam().getId().equals(teamId)) {
            List<Member> teamMembers = participationRepository.findAllTeamMembers(teamId);
            List<ParticipatedTeamMemberResponse.TeamMember> teamMemberList = new ArrayList<>();
            for (Member teamMember : teamMembers) {
                if (!teamMember.getId().equals(member.getId())) {
                    ParticipatedTeamMemberResponse.TeamMember participatedMember = ParticipatedTeamMemberResponse.TeamMember.builder()
                            .memberId(teamMember.getId())
                            .nickName(teamMember.getNickname())
                            .build();
                    teamMemberList.add(participatedMember);
                }
            }
            teamRepository.deleteById(teamId);
            Member my = memberRepository.findById(member.getId()).orElseThrow(
                    () -> new IllegalArgumentException("해당 맴버를 찾을 수 없습니다."));
            my.setOpenTeam(null);
            return ParticipatedTeamMemberResponse.builder()
                    .teamId(teamId)
                    .teamMemberList(teamMemberList)
                    .build();
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    @Transactional
    public String registerTeamImage(MultipartFile teamImageFile) {
        if (teamImageFile == null || teamImageFile.isEmpty()) {
            return null;
        }
        final Map<String, String> uploadFile = awsS3Service.uploadFile(teamImageFile);
        return uploadFile.get("url");
    }

    public TeamDuplicateResponse checkDuplicatedTeamName(TeamNameDuplicateRequest teamNameDuplicateRequest, Member member) {
        TeamDuplicateResponse teamDuplicateResponse = TeamDuplicateResponse.builder()
                .exist(true)
                .message("중복된 팀 이름입니다.")
                .build();
        if (teamRepository.findByTeamName(teamNameDuplicateRequest.getTeamName()).orElse(null) == null) {
            teamDuplicateResponse.updateExist(false);
            teamDuplicateResponse.updateMessage("사용 가능한 팀 이름입니다.");
        }
        return teamDuplicateResponse;
    }
}
