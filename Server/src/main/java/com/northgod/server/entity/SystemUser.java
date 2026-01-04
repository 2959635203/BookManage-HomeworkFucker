package com.northgod.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")  // 修改为 users，避免保留关键字
public class SystemUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 20)
    private String role = "STAFF"; // ADMIN, STAFF

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "is_active")
    private Boolean isActive = true;
}