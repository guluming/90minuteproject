package me.coldrain.ninetyminute.service;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.dto.request.ApprovedMatchRequest;
import me.coldrain.ninetyminute.dto.request.MatchMemberRequest;
import me.coldrain.ninetyminute.dto.request.MatchResultRequest;
import me.coldrain.ninetyminute.dto.request.MatchScoreRequest;
import me.coldrain.ninetyminute.dto.response.*;
import me.coldrain.ninetyminute.entity.*;
import me.coldrain.ninetyminute.repository.*;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingService {

    private final MemberRepository memberRepository;
    private final ParticipationRepository participationRepository;
    private final TeamRepository teamRepository;
    private final ApplyRepository applyRepository;
    private final BeforeMatchingRepository beforeMatchingRepository;
    private final FieldMemberRepository fieldMemberRepository;
    private final AfterMatchingRepository afterMatchingRepository;
    private final SubstituteRepository substituteRepository;
    private final ScorerRepository scorerRepository;
    private final HistoryRepository historyRepository;

    @Transactional
    public String approveApplyMatch(Long applyTeamId, Long applyId, ApprovedMatchRequest approvedMatchRequest, Member member) {
        if (!member.getOpenTeam().getId().equals(applyTeamId)) {
            String teamName = teamRepository.findByIdAndDeletedFalse(member.getOpenTeam().getId()).orElseThrow(
                    () -> new IllegalArgumentException("팀을 찾을 수 없습니다.")).getName();

            String opposingTeamName = teamRepository.findByIdAndDeletedFalse(applyTeamId).orElseThrow(
                    () -> new IllegalArgumentException("팀을 찾을 수 없습니다.")).getName();

            Apply applyMatch = applyRepository.findByIdAndMatchesStatusFalse(applyId).orElseThrow(
                    () -> new IllegalArgumentException("신청한 대결을 찾을 수 없습니다.")
            );

            if (!applyMatch.getApproved()) {
                applyMatch.changeApproved(true);

                BeforeMatching beforeMatching = BeforeMatching.builder()
                        .apply(applyMatch)
                        .matchDate(approvedMatchRequest.getMatchDate())
                        .location(approvedMatchRequest.getMatchLocation())
                        .opposingTeamName(opposingTeamName)
                        .teamName(teamName)
                        .build();

                beforeMatchingRepository.save(beforeMatching);
                return "완료됐습니다.";
            } else throw new IllegalArgumentException("이미 이 대결은 수락되었습니다.");
        } else throw new IllegalArgumentException("자신의 팀에 대결 수락은 불가능합니다.");
    }

    @Transactional
    public void rejectApplyMatch(Long applyTeamId, Long applyId, Member member) {
        if (!member.getOpenTeam().getId().equals(applyTeamId)) {
            Apply applyMatch = applyRepository.findById(applyId).orElseThrow(
                    () -> new IllegalArgumentException("신청한 대결이 존재하지 않습니다.")
            );
            if (applyMatch.getApproved()) {
                applyRepository.delete(applyMatch);
            } else throw new IllegalArgumentException("성사된 대결이 아니므로, 해당 대결이 존재하지 않아 거절할 수 없습니다.");
        } else throw new IllegalArgumentException("같은 팀 끼리의 대결은 존재 하지 않습니다.");
    }


    public List<OfferMatchResponse> searchOfferMatches(Long teamId, Member member) {
        List<OfferMatchResponse> offerMatchResponseList = new ArrayList<>();
        if (member.getOpenTeam().getId().equals(teamId)) {
            List<Apply> applyList = applyRepository.findAllByTeamIdOrderByCreatedDate(teamId);
            // 해당 팀에 신청된 경기가 없어도 null이 전달되야한다.
            if (!applyList.isEmpty()) {
                for (Apply apply : applyList) {
                    Member opposingTeamCaptain = memberRepository.findByOpenTeam(apply.getApplyTeam().getId()).orElse(null);
                    if (opposingTeamCaptain == null) continue;
                    OfferMatchResponse offerMatchResponse = OfferMatchResponse.builder()
                            .applyId(apply.getId())
                            .opposingTeamId(apply.getApplyTeam().getId())
                            .opposingTeamName(apply.getApplyTeam().getName())
                            .contact(opposingTeamCaptain.getContact())
                            .phone(opposingTeamCaptain.getPhone())
                            .opposingTeamPoint(apply.getApplyTeam().getRecord().getWinPoint())
                            .winRate(apply.getApplyTeam().getRecord().getWinRate())
                            .greeting(apply.getGreeting())
                            .createdDate(apply.getCreatedDate())
                            .modifiedDate(apply.getModifiedDate())
                            .build();
                    offerMatchResponseList.add(offerMatchResponse);
                }
            }
            return offerMatchResponseList;
        } else throw new IllegalArgumentException("이 팀의 주장이 아닙니다.");
    }

    public List<MatchResponse> searchMatches(Long teamId, Member member) {
        Participation participation = participationRepository.findByMemberIdAndTeamIdTrue(member.getId(), teamId).orElse(null);
        List<Apply> applyList = applyRepository.findAllByApprovedMatches(teamId);    // approved = true
        List<MatchResponse> matchResponseList = new ArrayList<>();
        if (participation != null) {
            for (Apply apply : applyList) {
                BeforeMatching beforeMatching = beforeMatchingRepository.findByApplyId(apply.getId()).orElseThrow(
                        () -> new IllegalArgumentException("성사된 대결이 존재하지 않습니다."));
                LocalDateTime from = LocalDateTime.now();
                LocalDateTime to = LocalDateTime.ofInstant(beforeMatching.getMatchDate().toInstant(), ZoneId.systemDefault());
                if (apply.getTeam().getId().equals(teamId)) {   // 내 팀이 home team 인 경우
                    MatchResponse matchResponse = MatchResponse.builder()
                            .matchId(beforeMatching.getId())
                            .teamId(teamId)
                            .isCaptain(false)
                            .opposingTeamId(apply.getApplyTeam().getId())
                            .opposingTeamName(apply.getApplyTeam().getName())
                            .opposingTeamMemberCount(participationRepository.findAllByTeamIdTrue(apply.getApplyTeam().getId()).size())
                            .opposingTeamPoint(apply.getApplyTeam().getRecord().getWinPoint())
                            .opposingTeamTotalGameCount(apply.getApplyTeam().getRecord().getTotalGameCount())
                            .opposingTeamWinRate(apply.getApplyTeam().getRecord().getWinRate())
                            .opposingTeamWinCount(apply.getApplyTeam().getRecord().getWinCount())
                            .opposingTeamDrawCount(apply.getApplyTeam().getRecord().getDrawCount())
                            .opposingTeamLoseCount(apply.getApplyTeam().getRecord().getLoseCount())
                            .matchLocation(beforeMatching.getLocation())
                            .contact(null)
                            .phone(null)
                            .matchDate(beforeMatching.calculatedDate())
                            .dDay(ChronoUnit.DAYS.between(from, to))
                            .createdDate(beforeMatching.getCreatedDate())
                            .modifiedDate(beforeMatching.getModifiedDate())
                            .matchStatus(true)
                            .build();
                    if (member.getOpenTeam() != null) {
                        if (member.getOpenTeam().getId().equals(teamId)) { // home team 이며 주장 일때
                            matchResponse.changeIsCaptain(true);
                            matchResponse.updateContact(memberRepository.findByOpenTeam(apply.getApplyTeam().getId()).orElseThrow(
                                    () -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getContact());
                            matchResponse.updatePhone(memberRepository.findByOpenTeam(apply.getApplyTeam().getId()).orElseThrow(
                                    () -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getPhone());
                        }
                    }
                    matchResponseList.add(matchResponse);
                } else {   // 내 팀이 away team 인 경우
                    MatchResponse matchResponse = MatchResponse.builder()
                            .matchId(beforeMatching.getId())
                            .teamId(teamId)
                            .isCaptain(false)
                            .opposingTeamId(apply.getTeam().getId())
                            .opposingTeamName(apply.getTeam().getName())
                            .opposingTeamMemberCount(participationRepository.findAllByTeamIdTrue(apply.getTeam().getId()).size())
                            .opposingTeamPoint(apply.getTeam().getRecord().getWinPoint())
                            .opposingTeamTotalGameCount(apply.getApplyTeam().getRecord().getTotalGameCount())
                            .opposingTeamWinRate(apply.getTeam().getRecord().getWinRate())
                            .opposingTeamWinCount(apply.getTeam().getRecord().getWinCount())
                            .opposingTeamDrawCount(apply.getTeam().getRecord().getDrawCount())
                            .opposingTeamLoseCount(apply.getTeam().getRecord().getLoseCount())
                            .matchLocation(beforeMatching.getLocation())
                            .contact(null)
                            .phone(null)
                            .matchDate(beforeMatching.calculatedDate())
                            .dDay(ChronoUnit.DAYS.between(from, to))
                            .createdDate(beforeMatching.getCreatedDate())
                            .modifiedDate(beforeMatching.getModifiedDate())
                            .matchStatus(true)
                            .build();
                    if (member.getOpenTeam() != null) {
                        if (member.getOpenTeam().getId().equals(teamId)) {  // away team 이며 주장 일때
                            matchResponse.changeIsCaptain(true);
                            matchResponse.updateContact(memberRepository.findByOpenTeam(apply.getTeam().getId()).orElseThrow(
                                    () -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getContact());
                            matchResponse.updatePhone(memberRepository.findByOpenTeam(apply.getTeam().getId()).orElseThrow(
                                    () -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")).getPhone());
                        }
                    }
                    matchResponseList.add(matchResponse);
                }
            }
            return matchResponseList;
        } else throw new IllegalArgumentException("이 팀에 참여한 멤버가 아닙니다.");
    }

    public List<ParticipationTeamMatchResponse> searchMyTeams(Long memberId, Member member) {
        List<Participation> ParticipationList = participationRepository.findAllByMembersTrue(memberId);
        List<ParticipationTeamMatchResponse> participationTeamMatchResponseList = new ArrayList<>();

        for (Participation participation : ParticipationList) {
            Boolean isCaptain = memberRepository.findById(memberId).orElseThrow(
                    () -> new IllegalArgumentException("이 회원은 존재하지 않습니다.")).getOpenTeam().getId().equals(participation.getTeam().getId());
            Integer teamMemberCnt = participationRepository.findAllByTeamIdTrue(participation.getTeam().getId()).size();
            ParticipationTeamMatchResponse participationTeamMatchResponse = ParticipationTeamMatchResponse.builder()
                    .teamId(participation.getTeam().getId())
                    .isCaptain(isCaptain)
                    .teamName(participation.getTeam().getName())
                    .teamMemberCount(teamMemberCnt)
                    .teamPoint(participation.getTeam().getRecord().getWinPoint())
                    .teamWinRate(participation.getTeam().getRecord().getWinRate())
                    .teamTotalGameCount(participation.getTeam().getRecord().getTotalGameCount())
                    .teamWinCount(participation.getTeam().getRecord().getWinCount())
                    .teamDrawCount(participation.getTeam().getRecord().getDrawCount())
                    .teamLoseCount(participation.getTeam().getRecord().getLoseCount())
                    .matchLocation(participation.getTeam().getMainArea())
                    .createdDate(participation.getCreatedDate())
                    .modifiedDate(participation.getModifiedDate())
                    .matchStatus(false)
                    .build();
            List<BeforeMatching> beforeMatchingList = beforeMatchingRepository.findAllMatches(participation.getTeam().getId());
            if (!beforeMatchingList.isEmpty()) {
                participationTeamMatchResponse.changeMatchStatus(true);
                participationTeamMatchResponseList.add(participationTeamMatchResponse);
            } else participationTeamMatchResponseList.add(participationTeamMatchResponse);
        }
        return participationTeamMatchResponseList;
    }

    public MatchResponse searchApprovedMatchDetail(Long teamId, Long matchId, Member member) {
        Participation participation = participationRepository.findByMemberIdAndTeamIdTrue(member.getId(), teamId).orElse(null);

        if (participation != null || member.getOpenTeam().getId().equals(teamId)) { // 팀의 멤버인지 확인, 주장일 때 participation 에 등록 되지 않는 것을 고려해야한다.
            BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                    () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
            Member captainMember = memberRepository.findByOpenTeam(beforeMatching.getApply().getApplyTeam().getId()).orElseThrow(() -> new IllegalAccessError(
                    "해당 멤버을 찾을 수 없거나 소속된 팀이 해체되었습니다."));
            Team opposingTeam = beforeMatching.getApply().getApplyTeam();
            LocalDateTime from = LocalDateTime.now();
            LocalDateTime to = LocalDateTime.ofInstant(beforeMatching.getMatchDate().toInstant(), ZoneId.systemDefault());
            MatchResponse matchResponse = MatchResponse.builder()
                    .matchId(beforeMatching.getId())
                    .isCaptain(false)
                    .opposingTeamId(opposingTeam.getId())
                    .opposingTeamName(opposingTeam.getName())
                    .opposingTeamPoint(opposingTeam.getRecord().getWinPoint())
                    .opposingTeamTotalGameCount(opposingTeam.getRecord().getTotalGameCount())
                    .opposingTeamWinRate(opposingTeam.getRecord().getWinRate())
                    .opposingTeamWinCount(opposingTeam.getRecord().getWinCount())
                    .opposingTeamDrawCount(opposingTeam.getRecord().getDrawCount())
                    .opposingTeamLoseCount(opposingTeam.getRecord().getLoseCount())
                    .contact(captainMember.getContact())
                    .phone(captainMember.getPhone())
                    .matchDate(beforeMatching.calculatedDate())
                    .dDay(ChronoUnit.DAYS.between(from, to))
                    .matchLocation(beforeMatching.getLocation())
                    .createdDate(beforeMatching.getCreatedDate())
                    .modifiedDate(beforeMatching.getModifiedDate())
                    .matchStatus(beforeMatching.getApply().getApproved())
                    .build();
            if (member.getOpenTeam().getId().equals(teamId)) { // team의 주장의 경우
                matchResponse.changeIsCaptain(true);
            }
            return matchResponse;
        } else throw new IllegalArgumentException("이 팀의 멤버가 아닙니다.");
    }

    @Transactional
    public void makeTeamFormation(Long teamId, Long matchId, List<MatchMemberRequest> matchMemberRequestList, Member member) {
        if (teamId.equals(member.getOpenTeam().getId())) {
            if (matchMemberRequestList.size() > 4) {
                BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                        () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다.")
                );
                if (teamId.equals(beforeMatching.getApply().getTeam().getId()) || teamId.equals(beforeMatching.getApply().getApplyTeam().getId())) {
                    Team team = teamRepository.findById(member.getOpenTeam().getId()).orElseThrow(
                            () -> new IllegalArgumentException("해당 팀을 찾을 수 없습니다.")
                    );
                    for (MatchMemberRequest matchMemberRequest : matchMemberRequestList) {
                        FieldMember fieldMember = FieldMember.builder()
                                .position(matchMemberRequest.getPosition())
                                .anonymous(matchMemberRequest.getAnonymous())
                                .member(null)
                                .team(team)
                                .beforeMatching(beforeMatching)
                                .afterMatching(null)
                                .build();
                        if (!matchMemberRequest.getAnonymous()) {
                            Member registerMember = memberRepository.findById(matchMemberRequest.getMemberId()).orElseThrow(
                                    () -> new IllegalArgumentException("해당 맴버를 찾을 수 없습니다."));
                            fieldMember.changeMember(registerMember);
                        }
                        fieldMemberRepository.save(fieldMember);
                    }
                } else throw new IllegalArgumentException("해당 팀은 성사된 대결의 팀이 아닙니다.");
            } else throw new IllegalArgumentException("최소 5명 이상의 선발 선수를 등록해 주세요.");
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    public MatchMemberResponse showFormation(Long teamId, Long matchId, Member member) {
        List<FieldMember> fieldMemberList = fieldMemberRepository.findAllByMatchFieldMembers(teamId, matchId);
        List<MatchMemberRequest> fieldMembers = new ArrayList<>();
        for (FieldMember fieldMember : fieldMemberList) {
            MatchMemberRequest matchMemberRequest = MatchMemberRequest.builder()
                    .memberId(null)
                    .memberProfileUrl(null)
                    .position(fieldMember.getPosition())
                    .anonymous(fieldMember.getAnonymous())
                    .build();
            if (!fieldMember.getAnonymous()) {
                matchMemberRequest.setMemberId(fieldMember.getMember().getId());
                matchMemberRequest.setMemberProfileUrl(fieldMember.getMember().getProfileUrl());
            }
            fieldMembers.add(matchMemberRequest);
        }
        return new MatchMemberResponse(matchId, fieldMembers);
    }

    @Transactional
    public void cancelApprovedMatch(Long matchId, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId()) || member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
            applyRepository.delete(beforeMatching.getApply());
            beforeMatchingRepository.delete(beforeMatching);
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    @Transactional
    public void confirmEndMatch(Long matchId, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다.")
        );

        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId())) {
            beforeMatching.getApply().changeEndMatchStatus(true);
        } else if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
            beforeMatching.getApply().changeOpposingTeamEndMatchStatus(true);
        } else throw new IllegalArgumentException("해당 대결 진행 중인 팀의 주장이 아닙니다.");
    }

    @Transactional
    public void writeMatchScore(Long matchId, MatchScoreRequest matchScoreRequest, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId())) {
            AfterMatching afterMatching = AfterMatching.builder()
                    .beforeMatching(beforeMatching)
                    .mvpNickname(null)
                    .moodMaker(null)
                    .score(matchScoreRequest.getTeamScore())
                    .opponentScore(matchScoreRequest.getOpponentScore())
                    .admitStatus(false)
                    .build();
            afterMatchingRepository.save(afterMatching);
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    public MatchScoreResponse showMatchScore(Long matchId, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId()) || member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
            AfterMatching afterMatching = afterMatchingRepository.findByBeforeMatchingIdAdmitStatusFalse(matchId).orElseThrow(
                    () -> new IllegalArgumentException("해당 대결을 찾을 수 없습니다.")
            );
            return MatchScoreResponse.builder()
                    .matchId(matchId)
                    .teamId(beforeMatching.getApply().getTeam().getId())
                    .opponentTeamId(beforeMatching.getApply().getApplyTeam().getId())
                    .teamScore(afterMatching.getScore())
                    .opponentTeamScore(afterMatching.getOpponentScore())
                    .build();
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    @Transactional
    public MatchScoreResponse confirmMatchScore(Long matchId, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId()) || member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
            AfterMatching afterMatching = afterMatchingRepository.findByBeforeMatchingIdAdmitStatusFalse(matchId).orElseThrow(
                    () -> new IllegalArgumentException("해당 대결을 찾을 수 없습니다.")
            );
            afterMatching.changeAdmitStatus(true);
            return MatchScoreResponse.builder()
                    .matchId(matchId)
                    .teamId(beforeMatching.getApply().getTeam().getId())
                    .opponentTeamId(beforeMatching.getApply().getApplyTeam().getId())
                    .teamScore(afterMatching.getScore())
                    .opponentTeamScore(afterMatching.getOpponentScore())
                    .build();
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    @Transactional
    public MatchScoreResponse correctMatchScore(Long matchId, MatchScoreRequest matchScoreRequest, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId()) || member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
            AfterMatching afterMatching = afterMatchingRepository.findByBeforeMatchingIdAdmitStatusFalse(matchId).orElseThrow(
                    () -> new IllegalArgumentException("해댱 대결을 찾을 수 없습니다.")
            );
            afterMatching.correctScore(matchScoreRequest.getTeamScore(), matchScoreRequest.getOpponentScore());
            return MatchScoreResponse.builder()
                    .matchId(matchId)
                    .teamId(beforeMatching.getApply().getTeam().getId())
                    .opponentTeamId(beforeMatching.getApply().getApplyTeam().getId())
                    .teamScore(matchScoreRequest.getTeamScore())
                    .opponentTeamScore(matchScoreRequest.getOpponentScore())
                    .build();
        } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
    }

    @Transactional
    public void writeMatchResult(Long matchId, MatchResultRequest matchResultRequest, Member member) {
        Participation participation = participationRepository.findByTeamIdAndMemberId(member.getOpenTeam().getId(), member.getId()).orElse(null);
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        AfterMatching afterMatching = afterMatchingRepository.findByBeforeMatchingIdAdmitStatusTrue(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾을 수 없습니다."));
        List<FieldMember> fieldMembers = fieldMemberRepository.findAllByMatchFieldMembers(member.getOpenTeam().getId(), beforeMatching.getId());
        fieldMembers.forEach(fieldMember -> fieldMember.setAfterMatching(afterMatching));
        List<SubstituteMember> substituteMembers = new ArrayList<>();

        if (afterMatching.getAdmitStatus()) {
            if (participation != null) {
                for (MatchMemberRequest substitute : matchResultRequest.getSubstitutes()) {
                    SubstituteMember substituteMember = SubstituteMember.builder()
                            .member(null)
                            .team(member.getOpenTeam())
                            .afterMatching(afterMatching)
                            .position(substitute.getPosition())
                            .anonymous(true)
                            .build();
                    if (!substitute.getAnonymous()) {
                        substituteMember.setMember(memberRepository.findById(substitute.getMemberId()).orElseThrow(
                                () -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.")));
                        substituteMember.changeAnonymous(substitute.getAnonymous());
                    }
                    substituteRepository.save(substituteMember);
                    substituteMembers.add(substituteMember);
                }
                if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getTeam().getId())) {
                    if (afterMatching.getScore() == matchResultRequest.getScorers().size()) { // home team
                        for (MatchMemberRequest scorer : matchResultRequest.getScorers()) {
                            if (!scorer.getAnonymous()) {
                                FieldMember fieldMember = fieldMemberRepository.findByMemberIdAndTeamIdAndBeforeMatchingId(scorer.getMemberId(), member.getOpenTeam().getId(), beforeMatching.getId()).orElse(null);
                                SubstituteMember substituteMember = searchSubMember(substituteMembers, scorer.getMemberId());
                                if (fieldMember == null && substituteMember == null)
                                    throw new IllegalArgumentException("해당 선수는 팀에 소속되어 있지 않습니다.");
                                Scorer scorerMember = Scorer.builder()
                                        .fieldMember(fieldMember)
                                        .substituteMember(substituteMember)
                                        .afterMatching(afterMatching)
                                        .teamId(member.getOpenTeam().getId())
                                        .build();
                                scorerRepository.save(scorerMember);
                            } else {
                                Scorer scorerMember = Scorer.builder()
                                        .fieldMember(null)
                                        .substituteMember(null)
                                        .afterMatching(afterMatching)
                                        .teamId(beforeMatching.getApply().getTeam().getId())
                                        .build();
                                scorerRepository.save(scorerMember);
                            }
                        }
                    } else throw new IllegalArgumentException("득점과 득점자 수가 일치하지 않습니다.");
                    if (matchResultRequest.getMvpPlayer() != null) {
                        Member mvpPlayer = participationRepository.findByMemberIdAndTeamIdTrue(matchResultRequest.getMvpPlayer(), member.getOpenTeam().getId()).orElseThrow(
                                () -> new IllegalArgumentException("해당 팀에 속해 있는 선수가 아닙니다.")).getMember();
                        afterMatching.editMVPNickname(mvpPlayer.getNickname());
                        mvpPlayer.getAbility().updateMVPPoint();
                    }
                    if (matchResultRequest.getMoodMaker() != null) {
                        Member moodMaker = participationRepository.findByMemberIdAndTeamIdTrue(matchResultRequest.getMoodMaker(), member.getOpenTeam().getId()).orElseThrow(
                                () -> new IllegalArgumentException("해당 팀에 속해 있는 선수가 아닙니다.")).getMember();
                        afterMatching.editMoodMaker(moodMaker.getNickname());
                        moodMaker.getAbility().updateCharmingPoint();
                    }
                } else if (member.getOpenTeam().getId().equals(beforeMatching.getApply().getApplyTeam().getId())) {
                    if (afterMatching.getOpponentScore() == matchResultRequest.getScorers().size()) {
                        for (MatchMemberRequest scorer : matchResultRequest.getScorers()) {
                            if (!scorer.getAnonymous()) {
                                FieldMember fieldMember = fieldMemberRepository.findByMemberIdAndTeamIdAndBeforeMatchingId(scorer.getMemberId(), member.getOpenTeam().getId(), beforeMatching.getId()).orElse(null);
                                SubstituteMember substituteMember = searchSubMember(substituteMembers, scorer.getMemberId());
                                if (fieldMember == null && substituteMember == null)
                                    throw new IllegalArgumentException("해당 선수는 팀에 소속되어 있지 않습니다.");
                                Scorer scorerMember = Scorer.builder()
                                        .fieldMember(fieldMember)
                                        .substituteMember(substituteMember)
                                        .afterMatching(afterMatching)
                                        .teamId(member.getId())
                                        .build();
                                scorerRepository.save(scorerMember);
                            } else {
                                Scorer scorerMember = Scorer.builder()
                                        .fieldMember(null)
                                        .substituteMember(null)
                                        .afterMatching(afterMatching)
                                        .teamId(beforeMatching.getApply().getApplyTeam().getId())
                                        .build();
                                scorerRepository.save(scorerMember);
                            }
                        }
                    } else throw new IllegalArgumentException("득점과 득점자 수가 일치하지 않습니다.");
                    if (matchResultRequest.getMvpPlayer() != null) {
                        Member mvpPlayer = participationRepository.findByMemberIdAndTeamIdTrue(matchResultRequest.getMvpPlayer(), member.getOpenTeam().getId()).orElseThrow(
                                () -> new IllegalArgumentException("해당 팀에 속해 있는 선수가 아닙니다.")).getMember();
                        afterMatching.editOpponentMVPNickname(mvpPlayer.getNickname());
                        mvpPlayer.getAbility().updateMVPPoint();
                    }
                    if (matchResultRequest.getMoodMaker() != null) {
                        Member moodMaker = participationRepository.findByMemberIdAndTeamIdTrue(matchResultRequest.getMoodMaker(), member.getOpenTeam().getId()).orElseThrow(
                                () -> new IllegalArgumentException("해당 팀에 속해 있는 선수가 아닙니다.")).getMember();
                        afterMatching.editOpponentMoodMaker(moodMaker.getNickname());
                        moodMaker.getAbility().updateCharmingPoint();
                    }
                }
                distributePoint(member.getOpenTeam().getId(), afterMatching.getId());
                History history = historyRepository.findByBeforeMatchingIdAndAfterMatchingId(beforeMatching.getId(), afterMatching.getId()).orElse(null);
                if (history == null) {
                    History saveHistory = History.builder()
                            .beforeMatching(afterMatching.getBeforeMatching())
                            .afterMatching(afterMatching)
                            .team(afterMatching.getBeforeMatching().getApply().getTeam())
                            .opposingTeam(afterMatching.getBeforeMatching().getApply().getApplyTeam())
                            .build();
                    historyRepository.save(saveHistory);
                }
            } else throw new IllegalArgumentException("해당 팀의 주장이 아닙니다.");
        } else throw new IllegalArgumentException("상대 팀이 결과를 인정하지 않았습니다.");
    }

    @Transactional
    void distributePoint(Long teamId, Long afterMatchingId) {
        List<String> results = Arrays.asList("승리", "무승부", "패배");
        AfterMatching afterMatching = afterMatchingRepository.findById(afterMatchingId).orElseThrow(() -> new IllegalArgumentException("성사된 대결이 존재하지 않습니다."));
        Team team = teamRepository.findByIdAndDeletedFalse(teamId).orElseThrow(() -> new IllegalArgumentException("해당 팀을 찾을 수 없습니다."));
        team.getRecord().updateTotalGameCount();
        List<FieldMember> fieldMembers = fieldMemberRepository.findAllByMatchFieldMembersAndAnonymousFalse(team.getId(), afterMatching.getBeforeMatching().getId());

        if (team.getName().equals(afterMatching.getBeforeMatching().getTeamName())) {
            if (afterMatching.getScore() > afterMatching.getOpponentScore()) {
                afterMatching.editResult(results.get(0), results.get(2));
                for (FieldMember fieldMember : fieldMembers) {
                    distributePositionPoint(fieldMember);
                }
                List<SubstituteMember> substituteMembers = substituteRepository.findAllByMatchSubstituteMembersAndAnonymousFalse(team.getId(), afterMatching.getId());
                for (SubstituteMember substituteMember : substituteMembers) {
                    distributePositionPoint(substituteMember);
                }
                team.getRecord().updateWinCount();
                team.getRecord().updateWinRate(((double) (team.getRecord().getWinCount() / team.getRecord().getTotalGameCount())) * 100.0);
            } else if (afterMatching.getScore() < afterMatching.getOpponentScore()) {
                afterMatching.editResult(results.get(2), results.get(0));
                for (FieldMember fieldMember : fieldMembers) {
                    distributePositionPoint(fieldMember);
                }
                List<SubstituteMember> substituteMembers = substituteRepository.findAllByMatchSubstituteMembersAndAnonymousFalse(team.getId(), afterMatching.getId());
                for (SubstituteMember substituteMember : substituteMembers) {
                    distributePositionPoint(substituteMember);
                }
                team.getRecord().updateLoseCount();
                team.getRecord().updateWinRate(((double) ((team.getRecord().getWinCount() / team.getRecord().getTotalGameCount()))) * 100.0);
            } else {
                afterMatching.editResult(results.get(1), results.get(1));
                team.getRecord().updateDrawCount();
                team.getRecord().updateWinRate(((double) ((team.getRecord().getWinCount() / team.getRecord().getTotalGameCount()))) * 100.0);
            }
        } else if (team.getName().equals(afterMatching.getBeforeMatching().getOpposingTeamName())) {
            if (afterMatching.getScore() > afterMatching.getOpponentScore()) {
                afterMatching.editResult(results.get(0), results.get(2));
                team.getRecord().updateLoseCount();
                team.getRecord().updateWinRate(((double) ((team.getRecord().getWinCount() / team.getRecord().getTotalGameCount()))) * 100.0);
            } else if (afterMatching.getScore() < afterMatching.getOpponentScore()) {
                afterMatching.editResult(results.get(2), results.get(0));
                for (FieldMember fieldMember : fieldMembers) {
                    distributePositionPoint(fieldMember);
                }
                List<SubstituteMember> substituteMembers = substituteRepository.findAllByMatchSubstituteMembersAndAnonymousFalse(team.getId(), afterMatching.getId());
                for (SubstituteMember substituteMember : substituteMembers) {
                    distributePositionPoint(substituteMember);
                }
                team.getRecord().updateWinCount();
                team.getRecord().updateWinRate(((double) ((team.getRecord().getWinCount() / team.getRecord().getTotalGameCount()))) * 100.0);
            } else {
                afterMatching.editResult(results.get(1), results.get(1));
                team.getRecord().updateDrawCount();
                team.getRecord().updateWinRate(((double) ((team.getRecord().getWinCount() / team.getRecord().getTotalGameCount()))) * 100.0);
            }
        }
    }

    @Transactional
    void distributePositionPoint(FieldMember fieldMember) {
        switch (fieldMember.getPosition()) {
            case "striker":
                fieldMember.getMember().getAbility().updateStrikePoint();
                break;
            case "midfielder":
                fieldMember.getMember().getAbility().updateMidfielderPoint();
                break;
            case "defender":
                fieldMember.getMember().getAbility().updateDefenderPoint();
                break;
            case "goalkeeper":
                fieldMember.getMember().getAbility().updateGoalkeeperPoint();
                break;
        }
    }

    @Transactional
    void distributePositionPoint(SubstituteMember substituteMember) {
        switch (substituteMember.getPosition()) {
            case "striker":
                substituteMember.getMember().getAbility().updateStrikePoint();
                break;
            case "midfielder":
                substituteMember.getMember().getAbility().updateMidfielderPoint();
                break;
            case "defender":
                substituteMember.getMember().getAbility().updateDefenderPoint();
                break;
            case "goalkeeper":
                substituteMember.getMember().getAbility().updateGoalkeeperPoint();
                break;
            default:
                break;
        }
    }

    private SubstituteMember searchSubMember(List<SubstituteMember> substituteMembers, Long memberId) {
        for (SubstituteMember substituteMember : substituteMembers) {
            if (!substituteMember.getAnonymous()) {
                if (substituteMember.getMember().getId().equals(memberId)) {
                    return substituteMember;
                }
                break;
            }
        }
        return null;
    }
}
