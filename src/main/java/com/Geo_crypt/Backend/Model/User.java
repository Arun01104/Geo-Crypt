package com.Geo_crypt.Backend.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true)
    private String username;


    @Column(nullable = false)
    private String email;


    @Column(nullable = false)
    private String passwordHash;


    private Instant createdAt = Instant.now();
}
