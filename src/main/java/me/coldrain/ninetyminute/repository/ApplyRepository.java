package me.coldrain.ninetyminute.repository;

import me.coldrain.ninetyminute.entity.Apply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ApplyRepository extends JpaRepository<Apply, Long> {

    @Query("SELECT a FROM Apply a WHERE a.applyTeam.id = :applyTeamId AND a.team.id = :teamId")
    Optional<Apply> findByApplyTeamIdAndTeamId(final Long applyTeamId, final Long teamId);

//    @Query("select a from Apply a where a.team.id =: teamId order by a.createdDate desc ")
//    List<Apply> findAllByTeamId(final Long teamId);

    List<Apply> findAllByTeamIdOrderByCreatedDate(final Long teamId);
}
