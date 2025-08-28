package com.Geo_crypt.Backend.repository;

import com.Geo_crypt.Backend.Model.FileLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileLog,Long> {
}
