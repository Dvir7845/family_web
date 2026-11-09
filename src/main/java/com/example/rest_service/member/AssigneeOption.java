package com.example.rest_service.member;

public record AssigneeOption(Long id, String username, boolean driver) {
    public AssigneeOption(Long id, String username) { this(id, username, false); }
}