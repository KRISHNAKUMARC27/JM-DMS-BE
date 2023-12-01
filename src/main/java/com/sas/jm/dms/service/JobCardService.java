package com.sas.jm.dms.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.JobCard;
import com.sas.jm.dms.entity.JobCardCounters;
import com.sas.jm.dms.entity.JobCardInfo;
import com.sas.jm.dms.entity.JobSpares;
import com.sas.jm.dms.entity.JobSparesInfo;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.repository.JobCardRepository;
import com.sas.jm.dms.repository.JobSparesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobCardService {

	private final JobCardRepository jobCardRepository;
	private final JobSparesRepository jobSparesRepository;
	private final SparesService sparesService;

	private final MongoTemplate mongoTemplate;

	public int getNextSequence(String sequenceName) {
		// Find the counter document and increment its sequence_value atomically
		Query query = new Query(Criteria.where("_id").is(sequenceName));
		Update update = new Update().inc("sequenceValue", 1);
		FindAndModifyOptions findAndModifyOptions = FindAndModifyOptions.options().returnNew(true);

		JobCardCounters counter = mongoTemplate.findAndModify(query, update, findAndModifyOptions,
				JobCardCounters.class);

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
		jobCard.setJobCreationDate(LocalDateTime.now());
		return jobCardRepository.save(jobCard);
	}

	public List<?> findAllByJobStatus(String status) {
		return jobCardRepository.findAllByJobStatusOrderByIdDesc(status);
	}

	public JobCard update(JobCard jobCard) {
		return jobCardRepository.save(jobCard);
	}

	public synchronized JobSpares updateJobSpares(JobSpares jobSpares) throws Exception {
		List<JobSparesInfo> jobSparesInfoList = jobSpares.getJobSparesInfo();
		for (JobSparesInfo jobSparesInfo : jobSparesInfoList) {
			SparesInventory spares = sparesService.findById(jobSparesInfo.getSparesId());
			if (spares != null) {
				if (jobSparesInfo.getQty().compareTo(spares.getQty()) > 0) {
					throw new Exception("Quantity of " + spares.getDesc() + " in Spares inventory (" + spares.getQty()
							+ ") is lesser than quantity of spares used for job (" + jobSparesInfo.getQty() + ")");
				} else {
					BigDecimal result = spares.getQty().subtract(jobSparesInfo.getQty());
					spares.setQty(result);
					spares.setAmount(spares.getSellRate().multiply(result));
					sparesService.save(spares);
				}
			} else {
				throw new Exception(jobSparesInfo.getSparesAndLabour() + " is not found in Spares Inventory ");
				// should never come here
			}
		}
		return jobSparesRepository.save(jobSpares);
	}

	public JobSpares getJobSpares(String id) {
		return jobSparesRepository.findById(id)
				.orElse(JobSpares.builder().jobSparesInfo(new ArrayList<>()).jobLaborInfo(new ArrayList<>()).build());
	}

	public synchronized JobCard updateJobStatus(JobCard jobCard) throws Exception {
		JobCard origJobCard = jobCardRepository.findById(jobCard.getId()).orElse(null);
		if (origJobCard != null) {
			if (jobCard.getJobStatus().equals("CLOSED")) {
				List<JobCardInfo> jobInfoList = origJobCard.getJobInfo();
				for (JobCardInfo jobInfo : jobInfoList) {
					if (!jobInfo.getCompleted().equals("Completed")) { // "Completed" string in UI also.
						throw new Exception(jobInfo.getComplaints() + " is not yet completed for the JobId "
								+ origJobCard.getJobId());
					}
				}
			}
		} else {
			throw new Exception("Invalid jobCard " + jobCard.getJobId());
			// should never come here
		}
		origJobCard.setJobStatus(jobCard.getJobStatus());
		return jobCardRepository.save(origJobCard);
	}
}
