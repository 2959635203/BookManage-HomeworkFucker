package com.northgod.client.model;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String fullName;
    private String role; // ADMIN, STAFF
    private String token;
}