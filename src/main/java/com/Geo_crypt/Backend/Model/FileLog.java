package com.Geo_crypt.Backend.Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "file_log")
@Data
public class FileLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String username;
    private String objectName;
    private String operation;
    private boolean success;
    private String latitude;
    private String longitude;
    private Instant timestamp = Instant.now();
}
