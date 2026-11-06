package com.example.rest_service.member;

public record CurrentMemberResponse(
    Long id,
    String username,
    Role role
) {}