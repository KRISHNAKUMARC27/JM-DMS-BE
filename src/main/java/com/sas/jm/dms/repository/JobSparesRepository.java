package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.JobSpares;

public interface JobSparesRepository extends MongoRepository<JobSpares, String> {

}
