package me.coldrain.ninetyminute.service;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.dto.response.ParticipatedTeamMemberResponse;
import me.coldrain.ninetyminute.dto.response.TeamMemberOfferResponse;
import me.coldrain.ninetyminute.dto.response.TeamMemberResponse;
import me.coldrain.ninetyminute.entity.BeforeMatching;
import me.coldrain.ninetyminute.entity.Member;
import me.coldrain.ninetyminute.entity.Participation;
import me.coldrain.ninetyminute.entity.Team;
import me.coldrain.ninetyminute.repository.*;
import me.coldrain.ninetyminute.security.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TeamMemberService {
    private final ParticipationRepository participationRepository;
    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final BeforeMatchingRepository beforeMatchingRepository;
    private final FieldMemberRepository fieldMemberRepository;

    //팀 멤버 조회
    public ResponseEntity<?> teamMemberGet(Long teamId, Long memberId) {
        TeamMemberResponse teamMemberResponse = new TeamMemberResponse();

        Member captain = memberRepository.findByOpenTeam(teamId).orElse(null);

        if (captain == null) {
            return new ResponseEntity<>("존재하지 않는 팀입니다.", HttpStatus.BAD_REQUEST);
        } else if (captain.getAbility() == null) {
            return new ResponseEntity<>("팀 개설자가 비정상적 입니다.", HttpStatus.BAD_REQUEST);
        } else {
            teamMemberResponse.setTeamCaptain(captain.getId().equals(memberId));

            TeamMemberResponse.CaptainMember CaptainMember = new TeamMemberResponse.CaptainMember(
                    captain.getId(),
                    captain.getProfileUrl(),
                    captain.getNickname(),
                    captain.getAbility().getMvpPoint(),
                    captain.getPosition(),
                    captain.getAbility().getStrikerPoint(),
                    captain.getAbility().getMidfielderPoint(),
                    captain.getAbility().getDefenderPoint(),
                    captain.getAbility().getGoalkeeperPoint()
            );
            teamMemberResponse.setCaptain(CaptainMember);
        }

        List<Participation> teamMemberList = participationRepository.findAllByTeamIdTrue(teamId);
        List<TeamMemberResponse.StrikerMember> strikerMemberList = new ArrayList<>();
        List<TeamMemberResponse.MidfielderMember> midfielderMemberList = new ArrayList<>();
        List<TeamMemberResponse.DefenderMember> defenderMemberList = new ArrayList<>();
        List<TeamMemberResponse.GoalkeeperMember> goalkeeperMemberList = new ArrayList<>();

        for (Participation participation : teamMemberList) {
            String position = participation.getMember().getPosition();

            if (participation.getMember().getAbility() != null) {
                if (!participation.getMember().getId().equals(captain.getId())) {
                    switch (position) {
                        case "striker":
                            TeamMemberResponse.StrikerMember strikerMember = new TeamMemberResponse.StrikerMember(
                                    participation.getMember().getId(),
                                    participation.getMember().getProfileUrl(),
                                    participation.getMember().getNickname(),
                                    participation.getMember().getAbility().getMvpPoint(),
                                    participation.getMember().getAbility().getStrikerPoint()
                            );
                            strikerMemberList.add(strikerMember);
                            break;

                        case "midfielder":
                            TeamMemberResponse.MidfielderMember midfielderMember = new TeamMemberResponse.MidfielderMember(
                                    participation.getMember().getId(),
                                    participation.getMember().getProfileUrl(),
                                    participation.getMember().getNickname(),
                                    participation.getMember().getAbility().getMvpPoint(),
                                    participation.getMember().getAbility().getMidfielderPoint()
                            );
                            midfielderMemberList.add(midfielderMember);
                            break;

                        case "defender":
                            TeamMemberResponse.DefenderMember defenderMember = new TeamMemberResponse.DefenderMember(
                                    participation.getMember().getId(),
                                    participation.getMember().getProfileUrl(),
                                    participation.getMember().getNickname(),
                                    participation.getMember().getAbility().getMvpPoint(),
                                    participation.getMember().getAbility().getDefenderPoint()
                            );
                            defenderMemberList.add(defenderMember);
                            break;

                        case "goalkeeper":
                            TeamMemberResponse.GoalkeeperMember goalkeeperMember = new TeamMemberResponse.GoalkeeperMember(
                                    participation.getMember().getId(),
                                    participation.getMember().getProfileUrl(),
                                    participation.getMember().getNickname(),
                                    participation.getMember().getAbility().getMvpPoint(),
                                    participation.getMember().getAbility().getGoalkeeperPoint()
                            );
                            goalkeeperMemberList.add(goalkeeperMember);
                            break;
                    }
                }
            }
        }
        teamMemberResponse.setStriker(strikerMemberList);
        teamMemberResponse.setMidfielder(midfielderMemberList);
        teamMemberResponse.setDefender(defenderMemberList);
        teamMemberResponse.setGoalkeeper(goalkeeperMemberList);

        return new ResponseEntity<>(teamMemberResponse, HttpStatus.OK);
    }

    //신청한 팀원 목록
    public ResponseEntity<?> teamMemberOfferGet(Long teamId, UserDetailsImpl userDetails) {
        Long loginMemberOpenTeamId;

        if (userDetails.getUser().getOpenTeam() == null) {
            return new ResponseEntity<>("개설한 팀이 없습니다.", HttpStatus.BAD_REQUEST);
        } else if (userDetails.getUser().getAbility() == null) {
            return new ResponseEntity<>("회원정보를 입력해주세요.", HttpStatus.BAD_REQUEST);
        } else {
            loginMemberOpenTeamId = userDetails.getUser().getOpenTeam().getId();
        }

        if (loginMemberOpenTeamId.equals(teamId)) {
            TeamMemberOfferResponse teamMemberOfferResponse = new TeamMemberOfferResponse();

            Team teamOffer = teamRepository.findById(teamId).orElseThrow();
            teamMemberOfferResponse.setQuestion(teamOffer.getQuestion());

            List<TeamMemberOfferResponse.OfferedMember> OfferedMemberList = new ArrayList<>();
            List<Participation> teamMemberOffer = participationRepository.findAllByTeamIdFalse(teamId);
            for (int i = 0; i < teamMemberOffer.size(); i++) {
                TeamMemberOfferResponse.OfferedMember OfferedMember = new TeamMemberOfferResponse.OfferedMember(
                        teamMemberOffer.get(i).getMember().getId(),
                        teamMemberOffer.get(i).getMember().getProfileUrl(),
                        teamMemberOffer.get(i).getMember().getNickname(),
                        teamMemberOffer.get(i).getMember().getAbility().getMvpPoint(),
                        teamMemberOffer.get(i).getMember().getPosition(),
                        teamMemberOffer.get(i).getMember().getAbility().getStrikerPoint(),
                        teamMemberOffer.get(i).getMember().getAbility().getMidfielderPoint(),
                        teamMemberOffer.get(i).getMember().getAbility().getDefenderPoint(),
                        teamMemberOffer.get(i).getMember().getAbility().getGoalkeeperPoint(),
                        teamMemberOffer.get(i).getAnswer()
                );
                OfferedMemberList.add(OfferedMember);
            }
            teamMemberOfferResponse.setOfferedMembers(OfferedMemberList);
            return new ResponseEntity<>(teamMemberOfferResponse, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("해당 팀의 개설자가 아닙니다.", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public ParticipatedTeamMemberResponse searchSubstituteMember(Long matchId, Member member) {
        BeforeMatching beforeMatching = beforeMatchingRepository.findByBeforeMatchingId(matchId).orElseThrow(
                () -> new IllegalArgumentException("해당 대결을 찾지 못했거나 대결 팀 중 한팀이 해체되었습니다."));
        List<Long> fieldMembers = fieldMemberRepository.findAllMemberIdOfFieldMembers(member.getOpenTeam().getId(), matchId);
        List<Member> substituteMembers = participationRepository.findAllMembers(member.getOpenTeam().getId(), fieldMembers);
        List<ParticipatedTeamMemberResponse.TeamMember> teamMembers = new ArrayList<>();
        for (Member teamMember : substituteMembers) {
            ParticipatedTeamMemberResponse.TeamMember temp = ParticipatedTeamMemberResponse.TeamMember.builder()
                    .memberId(teamMember.getId())
                    .memberProfileUrl(teamMember.getProfileUrl())
                    .nickName(teamMember.getNickname())
                    .build();
            teamMembers.add(temp);
        }
        return ParticipatedTeamMemberResponse.builder()
                .matchId(matchId)
                .teamId(member.getOpenTeam().getId())
                .teamMemberList(teamMembers)
                .build();
    }
}
