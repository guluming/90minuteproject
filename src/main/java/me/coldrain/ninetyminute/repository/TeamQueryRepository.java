package me.coldrain.ninetyminute.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.coldrain.ninetyminute.dto.QTeamListSearch;
import me.coldrain.ninetyminute.dto.TeamListSearch;
import me.coldrain.ninetyminute.dto.TeamListSearchCondition;
import me.coldrain.ninetyminute.entity.Time;
import me.coldrain.ninetyminute.entity.Weekday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static me.coldrain.ninetyminute.entity.QParticipation.participation;
import static me.coldrain.ninetyminute.entity.QRecord.record;
import static me.coldrain.ninetyminute.entity.QTeam.team;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TeamQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final WeekdayRepository weekdayRepository;
    private final TimeRepository timeRepository;

    public Page<TeamListSearch> findAllTeamListSearch(
            final TeamListSearchCondition searchCondition,
            final Pageable pageable) {

        List<TeamListSearch> content;
        JPAQuery<TeamListSearch> query = queryFactory.select(
                        new QTeamListSearch(
                                team.id,
                                team.name,
                                JPAExpressions.select(participation.count().add(1)) // 주장 +1
                                        .from(participation)
                                        .where(participation.approved.eq(true)
                                                , participation.team.id.eq(team.id)),
                                team.mainArea,
                                team.preferredArea,
                                team.record.winRate,
                                team.recruit,
                                team.match,
                                team.record.totalGameCount,
                                team.record.winCount,
                                team.record.drawCount,
                                team.record.loseCount,
                                team.createdDate,
                                team.modifiedDate))
                .from(team)
                .innerJoin(team.record, record)
                .where(containsIgnoreCaseTeamName(searchCondition.getTeamName()),   // 팀 이름
                        containsAddress(searchCondition.getAddress()),  // 주소
                        eqMatch(searchCondition.getMatch()),    // 대결 등록 상태
                        eqRecruit(searchCondition.getRecruit()) // 모집 상태
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
//                .orderBy(team.createdDate.desc())
//                .orderBy(getOrderSpecifier(pageable.getSort()).toArray(OrderSpecifier[]::new));

        if (searchCondition.getWinRate() == null) {
            content = query.orderBy(team.createdDate.desc())
                    .fetch();
        } else if (searchCondition.getWinRate().equals("desc")) {
            content = query.orderBy(team.record.winRate.desc(), team.createdDate.desc())
                    .fetch();
        } else {
            content = query.orderBy(team.record.winRate.asc(), team.createdDate.desc())
                    .fetch();
        }

        for (TeamListSearch c : content) {
            final List<String> weekdays = this.findWeekdaysByTeamId(c.getTeamId());
            c.setWeekdays(weekdays);

            final List<String> timeList = this.findTimeAllByTeamId(c.getTeamId());
            c.setTime(timeList);
        }

        List<TeamListSearch> filteredContent = new ArrayList<>();
        if (searchCondition.getWeekdays() != null) {
            content.forEach(c -> {
                boolean anyMatch = c.getWeekdays()
                        .stream()
                        .anyMatch(wd -> searchCondition.getWeekdays().contains(wd));
                if (anyMatch) {
                    filteredContent.add(c);
                }
            });
        } else if (searchCondition.getTime() != null) {
            content.forEach(c -> {
                boolean anyMatch = c.getTime().stream()
                        .anyMatch(t -> searchCondition.getTime().contains(t));
                if (anyMatch) {
                    filteredContent.add(c);
                }
            });
        }

        content = new ArrayList<>(filteredContent.isEmpty() ? content : filteredContent);
        return new PageImpl<>(content, pageable, content.size());
    }

    private BooleanExpression eqMatch(Boolean match) {
        return match != null ? team.match.eq(match) : null;
    }

    private BooleanExpression eqRecruit(Boolean recruit) {
        return recruit != null ? team.recruit.eq(recruit) : null;
    }

    private BooleanExpression containsIgnoreCaseTeamName(String teamName) {
        return teamName != null ? team.name.containsIgnoreCase(teamName) : null;
    }

    private BooleanExpression containsAddress(String address) {
        return address != null ? team.mainArea.contains(address) : null;
    }

    private List<String> findWeekdaysByTeamId(Long teamId) {
        return weekdayRepository.findAllByTeamId(teamId)
                .stream()
                .map(Weekday::getWeekday)
                .collect(toList());
    }

    private List<String> findTimeAllByTeamId(Long teamId) {
        return timeRepository.findAllByTeamId(teamId)
                .stream()
                .map(Time::getTime)
                .collect(toList());
    }

}