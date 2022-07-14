package me.coldrain.ninetyminute.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
@Slf4j
public class ExceptionController {

//    private final Logger logger = LoggerFactory.getLogger(this.getClass());

//    @ExceptionHandler(CustomSignUpException.class)
//    @ResponseBody
//    public ResponseEntity<ErrorResponse> handleCustomSignUpException(CustomSignUpException ex){
//        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.SingUpDataValidException.getCode(), ErrorCode.SingUpDataValidException.getMessage());
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex){
        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.USERNAME_OR_PASSWORD_NOTFOUND.getCode(), ErrorCode.USERNAME_OR_PASSWORD_NOTFOUND.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

//    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
//    @ResponseBody
//    public ResponseEntity<ErrorResponse> handleForbiddenException(AuthenticationException ex){
//        ErrorResponse errorResponse = new ErrorResponse(ErrorCode.ForbiddenException.getCode(), ErrorCode.ForbiddenException.getMessage());
//        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
//    }
}