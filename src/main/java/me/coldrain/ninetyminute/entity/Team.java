package me.coldrain.ninetyminute.entity;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"weekdays", "timeList"})
@Getter
public class Team extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long id;

    private String name;
    private String mainArea;
    private Integer point;
    private String teamProfileUrl;
    private String introduce;

    private Boolean recruit;
    private Boolean match;

    @OneToMany(mappedBy = "team")
    private List<Weekday> weekdays = new ArrayList<>(); // ['MON', 'TUE']

    @OneToMany(mappedBy = "team")
    private List<Time> timeList = new ArrayList<>();    // ['am', 'pm']

    private String preferredArea;
    private String question;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private Record record;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id")
    private History history;

    public void changeRecruit(Boolean recruit) {
        this.recruit = recruit;
    }

    public void changeMatch(Boolean match) {
        this.match = match;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void modifyTeam(String teamImageFileUrl, String introduce, String mainArea, String preferredArea, List<String> weekdays, List<String> time) {
        this.teamProfileUrl = teamImageFileUrl;
        this.introduce = introduce;
        this.mainArea = mainArea;
        this.preferredArea = preferredArea;
        this.weekdays = weekdays.stream().map(wd -> new Weekday(wd, this))
                .collect(Collectors.toList());
        this.timeList = time.stream().map(t -> new Time(t, this))
                .collect(Collectors.toList());
    }

    public void addWeekday(Weekday weekday) {
        weekdays.add(weekday);
        weekday.changeWeekday(this);
    }

    public void addTime(Time time) {
        timeList.add(time);
        time.changeTime(this);
    }

    @Builder

    public Team(String name, String mainArea, Integer point, String teamProfileUrl, String introduce, Boolean recruit, List<Weekday> weekdays, List<Time> timeList, String preferredArea, String question, Record record, History history, Boolean match) {
        this.name = name;
        this.mainArea = mainArea;
        this.point = point;
        this.teamProfileUrl = teamProfileUrl;
        this.introduce = introduce;
        this.recruit = recruit;
        this.weekdays = weekdays;
        this.timeList = timeList;
        this.preferredArea = preferredArea;
        this.question = question;
        this.record = record;
        this.history = history;
        this.match = match;
    }
}
