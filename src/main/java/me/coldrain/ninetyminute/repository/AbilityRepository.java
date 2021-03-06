package me.coldrain.ninetyminute.repository;

import me.coldrain.ninetyminute.entity.Ability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbilityRepository extends JpaRepository<Ability, Long> {

    //TOP10 개인 랭킹 조회
    List<Ability> findFirst10ByOrderByMvpPointDesc();

    List<Ability> findFirst10ByOrderByStrikerPointDesc();

    List<Ability> findFirst10ByOrderByMidfielderPointDesc();

    List<Ability> findFirst10ByOrderByDefenderPointDesc();

    List<Ability> findFirst10ByOrderByGoalkeeperPointDesc();

    List<Ability> findFirst10ByOrderByCharmingPointDesc();

    //개별 랭킹 확인을 위한 포지션별 랭킹 전체 조회
    List<Ability> findAllByOrderByStrikerPointDesc();

    List<Ability> findAllByOrderByMidfielderPointDesc();

    List<Ability> findAllByOrderByDefenderPointDesc();

    List<Ability> findAllByOrderByGoalkeeperPointDesc();
}
