package me.coldrain.ninetyminute.repository;

import me.coldrain.ninetyminute.entity.FieldMember;
import me.coldrain.ninetyminute.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FieldMemberRepository extends JpaRepository<FieldMember, Long> {

    @Query("select fm from FieldMember fm where fm.member.id = :memberId and fm.team.id = :teamId")
    Optional<FieldMember> findByMemberIdAndTeamId(Long memberId, Long teamId);

    @Query("select fm from FieldMember fm where fm.member.id = :memberId and fm.team.id = :teamId and fm.beforeMatching.id = :beforeMatchingId")
    Optional<FieldMember> findByMemberIdAndTeamIdAndBeforeMatchingId(Long memberId, Long teamId, Long beforeMatchingId);

    @Query("select fm from FieldMember fm where fm.team.id = :teamId and fm.beforeMatching.id = :beforeMatchId and fm.anonymous = false ")
    List<FieldMember> findAllByMatchFieldMembersAndAnonymousFalse(Long teamId, Long beforeMatchId);

    @Query("select fm from FieldMember fm where fm.team.id = :teamId and fm.beforeMatching.id = :beforeMatchingId")
    List<FieldMember> findAllByMatchFieldMembers(Long teamId, Long beforeMatchingId);

    @Query("select fm from FieldMember fm where fm.team.id = :teamId and fm.afterMatching.id = :afterMatchingId")
    List<FieldMember> findAllByAfterMatchingFieldMembers(Long teamId, Long afterMatchingId);

    @Query("select fm from FieldMember fm where fm.member.id = :MemberId")
    List<FieldMember> findAllByGameFieldMembers(Long MemberId);

    @Query("select fm.member.id from FieldMember fm where fm.team.id = :teamId and fm.beforeMatching.id = :beforeMatchId and fm.anonymous = false")
    List<Long> findAllMemberIdOfFieldMembers(Long teamId, Long beforeMatchId);
}
