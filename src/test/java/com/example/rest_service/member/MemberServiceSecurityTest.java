package com.example.rest_service.member;

import com.example.rest_service.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    SecurityConfig.class,
    MemberService.class
})
class MemberServiceSecurityTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AppUserRepository userRepository;

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotCreateAccounts() {
        CreateMemberRequest request = new CreateMemberRequest(
            "new_member",
            "ExamplePass123!"
        );

        assertThrows(
            AccessDeniedException.class,
            () -> memberService.createMember(request)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    @WithMockUser(username = "dvir", roles = "ADMIN")
    void adminCreatesMemberWithHashedPassword() {
        CreateMemberRequest request = new CreateMemberRequest(
            "new_member",
            "ExamplePass123!"
        );

        AppUser savedUser = mock(AppUser.class);
        when(savedUser.getId()).thenReturn(2L);
        when(savedUser.getUsername()).thenReturn("new_member");

        when(userRepository.saveAndFlush(any(AppUser.class)))
            .thenReturn(savedUser);

        AssigneeOption result = memberService.createMember(request);

        ArgumentCaptor<AppUser> captor =
            ArgumentCaptor.forClass(AppUser.class);

        verify(userRepository).saveAndFlush(captor.capture());

        AppUser createdUser = captor.getValue();

        assertEquals("new_member", createdUser.getUsername());
        assertEquals(Role.MEMBER, createdUser.getRole());

        assertNotEquals(
            request.password(),
            createdUser.getPasswordHash()
        );

        assertTrue(passwordEncoder.matches(
            request.password(),
            createdUser.getPasswordHash()
        ));

        assertEquals(
            new AssigneeOption(2L, "new_member"),
            result
        );
    }
}