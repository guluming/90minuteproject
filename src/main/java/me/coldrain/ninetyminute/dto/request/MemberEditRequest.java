package me.coldrain.ninetyminute.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
public class MemberEditRequest {

    @JsonProperty("profileImageFile")
    private MultipartFile profileImageFile;

    @Pattern(regexp = "^[가-힣a-zA-Z]+$")
    @NotBlank
    @JsonProperty("nickname")
    private String nickname;

    @NotBlank
    @JsonProperty("position")
    private String position;

    @NotBlank
    @JsonProperty("contact")
    private String contact;

    @Pattern(regexp = "^\\d+$")
    @JsonProperty("phone")
    private String phone;
}
