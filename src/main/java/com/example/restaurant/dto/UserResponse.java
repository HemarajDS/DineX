package com.example.restaurant.dto;

import com.example.restaurant.entity.Role;

import java.util.List;

public class UserResponse {

    private String id;
    private String name;
    private String email;
    private Role role;
    private List<String> assignedTableNumbers;

    public UserResponse() {
    }

    public UserResponse(String id, String name, String email, Role role, List<String> assignedTableNumbers) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.assignedTableNumbers = assignedTableNumbers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<String> getAssignedTableNumbers() {
        return assignedTableNumbers;
    }

    public void setAssignedTableNumbers(List<String> assignedTableNumbers) {
        this.assignedTableNumbers = assignedTableNumbers;
    }
}
