package me.coldrain.ninetyminute.entity;


import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Getter
public class BeforeMatching extends TimeStamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "before_matching_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_id")
    private Apply apply;

    private Date matchDate;
    private String location;
    private String teamName;
    private String opposingTeamName;

    @Builder
    public BeforeMatching(Apply apply, Date matchDate, String location, String teamName, String opposingTeamName) {
        this.apply = apply;
        this.matchDate = matchDate;
        this.location = location;
        this.teamName = teamName;
        this.opposingTeamName = opposingTeamName;
    }
}
