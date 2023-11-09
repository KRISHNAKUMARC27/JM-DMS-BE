package com.sas.jm.dms.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.JobCard;

public interface JobCardRepository extends MongoRepository<JobCard, Integer> {
	
	List<JobCard> findAllByOrderByIdDesc();

}
