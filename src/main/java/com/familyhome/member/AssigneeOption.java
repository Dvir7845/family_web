package com.familyhome.member;

public record AssigneeOption(Long id, String username, boolean driver, Role role) {
    public AssigneeOption(Long id, String username, boolean driver) { this(id, username, driver, Role.MEMBER); }
    public AssigneeOption(Long id, String username) { this(id, username, false); }
}