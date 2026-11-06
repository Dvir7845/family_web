package com.example.rest_service.member;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public AssigneeOption createMember(
            @Valid @RequestBody CreateMemberRequest request) {
        return memberService.createMember(request);
    }

    @GetMapping("/me")
    public CurrentMemberResponse getMe(Principal principal) {
        return memberService.getCurrentMember(principal.getName());
    }
}