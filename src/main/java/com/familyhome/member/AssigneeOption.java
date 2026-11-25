package com.familyhome.member;

public record AssigneeOption(
        Long id, String username, boolean driver, boolean taskManager, Role role) {
    public AssigneeOption(Long id, String username, boolean driver, boolean taskManager) {
        this(id, username, driver, taskManager, Role.MEMBER);
    }

    public AssigneeOption(Long id, String username) {
        this(id, username, false, false, Role.MEMBER);
    }
}
