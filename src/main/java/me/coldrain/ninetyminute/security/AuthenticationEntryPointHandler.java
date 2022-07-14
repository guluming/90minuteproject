package me.coldrain.ninetyminute.security;

import lombok.RequiredArgsConstructor;
import me.coldrain.ninetyminute.exception.ErrorCode;
import org.json.JSONObject;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String exception = (String) request.getAttribute("exception");
        ErrorCode errorCode;

        // 토큰이 없는 경우 예외처리
        if(exception == null) {
            errorCode = ErrorCode.UNAUTHORIZED;
            setResponse(response, errorCode);
            return;
        }

        // 토큰이 만료된 경우 예외처리
        if(exception.equals("ExpiredJwtException")) {
            errorCode = ErrorCode.EXPIRED_JWT;
            setResponse(response, errorCode);
        }
    }

    private void setResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        JSONObject json = new JSONObject();
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        json.put("code", errorCode.getCode());
        json.put("message", errorCode.getMessage());
        response.getWriter().print(json);
    }
}
