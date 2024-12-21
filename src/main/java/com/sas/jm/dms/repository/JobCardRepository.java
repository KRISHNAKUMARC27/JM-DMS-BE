package com.sas.jm.dms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.JobCard;

public interface JobCardRepository extends MongoRepository<JobCard, String> {
	
	List<JobCard> findAllByOrderByIdDesc();
	List<JobCard> findAllByJobStatusOrderByIdDesc(String jobStatus);
    List<JobCard> findByJobCloseDateBetween(LocalDateTime start, LocalDateTime end);
    Long countByJobStatus(String jobStatus);
    List<JobCard> findByJobCloseDateBetweenAndJobStatusOrderByJobIdDesc(LocalDateTime start, LocalDateTime end, String jobStatus);


}
