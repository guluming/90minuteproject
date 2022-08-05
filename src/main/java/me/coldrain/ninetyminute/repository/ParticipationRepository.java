package me.coldrain.ninetyminute.repository;

import me.coldrain.ninetyminute.entity.Member;
import me.coldrain.ninetyminute.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    @Query("SELECT p FROM Participation p WHERE p.team.id = :teamId AND p.member.id = :memberId AND p.team.deleted = false")
    Optional<Participation> findByTeamIdAndMemberId(final Long teamId, final Long memberId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.approved = true AND p.team.deleted = false")
    List<Participation> findAllByMemberIdTrue(final Long memberId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.approved = false AND p.team.deleted = false")
    List<Participation> findAllByMemberIdFalse(final Long memberId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.team.id = :teamId AND p.approved = false AND p.team.deleted = false")
    Optional <Participation> findByTeamIdAndMemberIdFalse(final Long memberId, final Long teamId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.team.id = :teamId AND p.approved = true AND p.team.deleted = false")
    Optional<Participation> findByMemberIdAndTeamIdTrue(final Long memberId, final Long teamId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.approved = true AND p.team.deleted = false")
    List<Participation> findAllByMembersTrue(final Long memberId);

    @Query("SELECT p FROM Participation p WHERE p.team.id = :teamId AND p.approved = true AND p.team.deleted = false")
    List<Participation> findAllByTeamIdTrue(final Long teamId);

    @Query("SELECT p FROM Participation p WHERE p.team.id = :teamId AND p.approved = false AND p.team.deleted = false")
    List<Participation> findAllByTeamIdFalse(final Long teamId);

    @Query("SELECT p FROM Participation p WHERE p.member.id = :memberId AND p.approved = false AND p.team.deleted = false")
    List<Participation> findAllByMemberIdApprovedFalse(final Long memberId);

    @Query("SELECT p.member FROM Participation p WHERE p.team.id = :teamId AND p.approved = true AND p.team.deleted = false")
    List<Member> findAllTeamMembers(final Long teamId);

    @Query("select p.member from Participation p where p.team.id = :teamId and not p.member.id in :fieldMembers and p.approved = true")
    List<Member> findAllMembers(Long teamId, List<Long> fieldMembers);
}
