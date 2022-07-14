package me.coldrain.ninetyminute.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Getter
@NoArgsConstructor
@ToString
public class MemberEmailDuplicateRequest {
    @Pattern(regexp = "^[a-zA-Z0-9]+@[a-zA-Z0-9]+.[a-z]+$")
    @NotBlank
    private String email;
}
