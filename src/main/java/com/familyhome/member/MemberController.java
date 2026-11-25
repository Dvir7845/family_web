package com.familyhome.member;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Members", description = "Current member session, roles, flags, and account management")
public class MemberController {

    private final MemberService memberService;
    private final AccountService accounts;

    public MemberController(MemberService memberService, AccountService accounts) {
        this.memberService = memberService;
        this.accounts = accounts;
    }

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    public AssigneeOption createMember(@Valid @RequestBody CreateMemberRequest request) {
        return memberService.createMember(request);
    }

    public record DriverRequest(@NotNull Boolean driver) {}

    public record TaskManagerRequest(@NotNull Boolean taskManager) {}

    @PatchMapping("/members/{id}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDriver(@PathVariable Long id, @Valid @RequestBody DriverRequest request) {
        memberService.changeDriver(id, request.driver());
    }

    @PatchMapping("/members/{id}/task-manager")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeTaskManager(
            @PathVariable Long id, @Valid @RequestBody TaskManagerRequest request) {
        memberService.changeTaskManager(id, request.taskManager());
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
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        accounts.changePassword(principal.getName(), request);
        new SecurityContextLogoutHandler()
                .logout(
                        servletRequest,
                        servletResponse,
                        SecurityContextHolder.getContext().getAuthentication());
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
