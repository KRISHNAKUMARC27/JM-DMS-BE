package com.sas.jm.dms.service;

import java.util.List;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.JobCard;
import com.sas.jm.dms.entity.JobCardCounters;
import com.sas.jm.dms.repository.JobCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCardService {

	private final JobCardRepository jobCardRepository;

	private final MongoTemplate mongoTemplate;

	public int getNextSequence(String sequenceName) {
		// Find the counter document and increment its sequence_value atomically
		Query query = new Query(Criteria.where("_id").is(sequenceName));
		Update update = new Update().inc("sequenceValue", 1);
		FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().returnNew(true);

		JobCardCounters counter = mongoTemplate.findAndModify(query, update, findAndModifyOptions, JobCardCounters.class);

		if (counter == null) {
			throw new RuntimeException("Error getting next sequence for " + sequenceName);
		}

		return counter.getSequenceValue();
	}

	public List<?> findAll() {
		return jobCardRepository.findAllByOrderByIdDesc();
	}

	public JobCard save(JobCard jobCard) {
		jobCard.setJobId(getNextSequence("jobCardId"));
		return jobCardRepository.save(jobCard);
	}
}
