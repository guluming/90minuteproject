package me.coldrain.ninetyminute.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.coldrain.ninetyminute.dto.request.*;
import me.coldrain.ninetyminute.service.KakaoMemberService;
import me.coldrain.ninetyminute.service.MemberService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final KakaoMemberService kakaoMemberService;

    //회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> memberSignup(@RequestBody @Valid MemberRegisterRequest memberRegisterRequest) {
        return memberService.memberSignup(memberRegisterRequest);
    }

    //아이디 중복 확인
    @PostMapping("/email/exist")
    public ResponseEntity<?> duplicateCheckEmail(@RequestBody @Valid MemberEmailDuplicateRequest memberEmailDuplicateRequest) {
        return memberService.duplicateCheckEmail(memberEmailDuplicateRequest);
    }

    //닉네임 중복 확인
    @PostMapping("/nickname/exist")
    public ResponseEntity<?> duplicateCheckNickname(@RequestBody @Valid MemberNicknameDuplicateRequest memberNicknameDuplicateRequest) {
        return memberService.duplicateCheckNickname(memberNicknameDuplicateRequest);
    }

    //회원정보 수정
    @PatchMapping(value = "/{member_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> memberEdit(@PathVariable Long member_id,
                                        MemberEditRequest memberEditRequest) {

        log.info("memberEdit.memberEditRequest = {}", memberEditRequest);

        return memberService.memberEdit(member_id, memberEditRequest);
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<?> memberLogin(@RequestBody @Valid MemberLoginRequest memberLoginRequest) {
        return memberService.memberLogin(memberLoginRequest);
    }

    //소셜로그인
    @GetMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code) throws JsonProcessingException {
        //프론트엔드 인가코드 요청 방법
        //https://kauth.kakao.com/oauth/authorize?client_id=3c2e867a60400604cd64199c1ec0227a&redirect_uri=http://localhost:8080/api/members/kakao/login&response_type=code
        return kakaoMemberService.kakaoLogin(code);
    }
}
