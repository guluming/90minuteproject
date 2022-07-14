package me.coldrain.ninetyminute.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
    USERNAME_OR_PASSWORD_NOTFOUND (400, "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED (401, "로그인 후 이용가능합니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_JWT(444, "Access 토큰이 만료되었습니다. 재로그인을 진행해주세요", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "해당 요청에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    RE_LOGIN(445, "모든 토큰이 만료되었습니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED),
    ;

    @Getter
    private int code;

    @Getter
    private String message;

    @Getter
    private HttpStatus status;

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
