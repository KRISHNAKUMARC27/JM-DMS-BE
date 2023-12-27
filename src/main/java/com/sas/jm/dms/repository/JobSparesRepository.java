package com.sas.jm.dms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.JobSpares;

public interface JobSparesRepository extends MongoRepository<JobSpares, String> {
    List<JobSpares> findByJobCloseDateBetween(LocalDateTime start, LocalDateTime end);
    List<JobSpares> findByJobCloseDateNotNull();

}
