package com.familyhome.member;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import java.util.List;

@RestController
public class MemberController {

    private final MemberService memberService;
    private final AccountService accounts;

    public MemberController(MemberService memberService, AccountService accounts) {
        this.memberService = memberService;
        this.accounts = accounts;
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public AssigneeOption createMember(
            @Valid @RequestBody CreateMemberRequest request) {
        return memberService.createMember(request);
    }

    public record DriverRequest(@jakarta.validation.constraints.NotNull Boolean driver) {}

    @PatchMapping("/members/{id}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDriver(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        memberService.changeDriver(id, request.driver());
    }

    @PostMapping("/members/{id}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteMember(@PathVariable Long id) {
        accounts.promote(id);
    }

    @DeleteMapping("/members/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable Long id, Principal principal) {
        accounts.delete(id, principal.getName());
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, Principal principal,
            jakarta.servlet.http.HttpServletRequest servletRequest, jakarta.servlet.http.HttpServletResponse servletResponse) {
        accounts.changePassword(principal.getName(), request);
        new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(
            servletRequest, servletResponse, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    }

    @GetMapping("/me")
    public CurrentMemberResponse getMe(Principal principal) {
        return memberService.getCurrentMember(principal.getName());
    }

    @GetMapping("/members")
    public List<AssigneeOption> getMembers() {
        return memberService.getMembers();
    }
}