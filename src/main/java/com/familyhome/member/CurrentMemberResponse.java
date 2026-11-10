package com.familyhome.member;

public record CurrentMemberResponse(
    Long id,
    String username,
    Role role,
    boolean driver
) {}