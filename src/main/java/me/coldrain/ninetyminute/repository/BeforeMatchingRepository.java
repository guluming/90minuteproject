package me.coldrain.ninetyminute.repository;


import me.coldrain.ninetyminute.entity.AfterMatching;
import me.coldrain.ninetyminute.entity.BeforeMatching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BeforeMatchingRepository extends JpaRepository<BeforeMatching, Long> {

    @Query("select bm from BeforeMatching bm where bm.apply.team.id = :teamId and bm.apply.approved = true and bm.apply.team.deleted = false order by bm.createdDate desc")
    List<BeforeMatching> findAllByBeforeMatching(final Long teamId);

    @Query("select bm from BeforeMatching bm where bm.apply.team.id = :teamId and bm.apply.approved = true and bm.apply.endMatchStatus = true and bm.apply.opposingTeamEndMatchStatus = true order by bm.createdDate desc")
    List<BeforeMatching> findByRecentTeamBeforeMatching(Long teamId);

    @Query("select bm from BeforeMatching bm where bm.apply.applyTeam.id = :teamId and bm.apply.approved = true and bm.apply.endMatchStatus = true and bm.apply.opposingTeamEndMatchStatus = true  order by bm.createdDate desc")
    List<BeforeMatching> findByRecentOpposingTeamBeforeMatching(Long teamId);

    @Query("select bm from BeforeMatching bm where bm.apply.team.id = :teamId and bm.apply.approved = true and bm.apply.team.deleted = false order by bm.createdDate desc")
    Optional<BeforeMatching> findByRecentBeforeMatching(Long teamId);

    @Query("select bm from BeforeMatching bm where bm.apply.id = :applyId and bm.apply.team.deleted = false and bm.apply.applyTeam.deleted = false ")
    Optional<BeforeMatching> findByApplyId(final Long applyId);

    @Query("select bm from BeforeMatching bm where (bm.apply.team.id = :teamId or bm.apply.applyTeam.id = :teamId) and bm.apply.team.deleted = false and bm.apply.applyTeam.deleted = false and bm.apply.approved = true and bm.apply.endMatchStatus = false and bm.apply.opposingTeamEndMatchStatus = false order by bm.matchDate desc ")
    List<BeforeMatching> findAllMatches(final Long teamId);

    @Query("select bm from BeforeMatching bm where bm.id = :beforeMatchingId and bm.apply.team.deleted = false and bm.apply.applyTeam.deleted = false")
    Optional<BeforeMatching> findByBeforeMatchingId(final Long beforeMatchingId);
}
