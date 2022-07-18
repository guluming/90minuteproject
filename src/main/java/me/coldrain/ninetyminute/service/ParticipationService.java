package me.coldrain.ninetyminute.service;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.dto.request.TeamParticipateRequest;
import me.coldrain.ninetyminute.entity.Member;
import me.coldrain.ninetyminute.entity.Participation;
import me.coldrain.ninetyminute.entity.Team;
import me.coldrain.ninetyminute.repository.MemberRepository;
import me.coldrain.ninetyminute.repository.ParticipationRepository;
import me.coldrain.ninetyminute.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void participate(
            final Long teamId,
            final Member member,
            final TeamParticipateRequest request) {

        final Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("참여하려는 팀이 존재하지 않습니다."));

        if (team.getRecruit().equals(false)) {
            throw new IllegalArgumentException("팀원을 모집 중인 상태가 아닙니다.");
        }

        final boolean present = participationRepository.findByTeamIdAndMemberId(teamId, member.getId()).isPresent();
        if (present) {
            throw new IllegalArgumentException("이미 참여신청을 했습니다.");
        }

        final Participation participation = Participation.builder()
                .member(member)
                .team(team)
                .approved(false)
                .answer(request.getAnswer())
                .build();

        participationRepository.save(participation);
    }

    @Transactional
    public void approve(final Long teamId, final Long memberId, final Member member) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀이 존재하지 않습니다."));
        memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        final Participation participation = participationRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("참여 이력이 존재하지 않습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(teamId)) {
            throw new IllegalArgumentException("팀 개설자만 참여 승인을 할 수 있습니다.");
        }

        if (participation.getApproved().equals(true)) {
            throw new IllegalArgumentException("이미 승인된 회원입니다.");
        }

        participation.changeApproved(true);
    }

    @Transactional
    public void disapprove(final Long teamId, final Long memberId, final Member member) {
        final Participation participation = participationRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("참여 이력이 존재하지 않습니다."));

        final Long openTeamId = member.getOpenTeam().getId();
        if (!openTeamId.equals(teamId)) {
            throw new IllegalArgumentException("팀 개설자만 참여 승인을 할 수 있습니다.");
        }

        participationRepository.delete(participation);
    }
}
