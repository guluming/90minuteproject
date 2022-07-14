package me.coldrain.ninetyminute.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
public class TeamModifyRequest {

    private MultipartFile teamImageFile;
    private String introduce;
    private String mainArea;
    private String preferredArea;
    private List<String> weekdays;
    private List<String> time;

    @Builder
    public TeamModifyRequest(MultipartFile teamImageFile, String introduce, String mainArea, String preferredArea, List<String> weekdays, List<String> time) {
        this.teamImageFile = teamImageFile;
        this.introduce = introduce;
        this.mainArea = mainArea;
        this.preferredArea = preferredArea;
        this.weekdays = weekdays;
        this.time = time;
    }
}
