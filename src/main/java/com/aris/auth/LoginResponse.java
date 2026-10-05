package com.aris.auth;
public class LoginResponse {
    private String token;
    private Long id;
    private String name;
    private String email;
    private Role role;
    public LoginResponse(String token, Long id, String name, String email, Role role) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }
    public String getToken() {
        return token;
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public Role getRole() {
        return role;
    }
}
