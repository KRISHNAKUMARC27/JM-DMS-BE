package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sas.jm.dms.entity.JobCard;
import com.sas.jm.dms.entity.JobSpares;
import com.sas.jm.dms.service.JobCardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/jobCard")
@RequiredArgsConstructor
@Slf4j
public class JobCardController {

	private final JobCardService jobCardService;

	@GetMapping
	public List<?> findAll() {
		return jobCardService.findAll();
	}

	@PostMapping
	public JobCard save(@RequestBody JobCard jobCard) {
		return jobCardService.save(jobCard);
	}

	@GetMapping("/status/{status}")
	public List<?> findAllByJobStatus(@PathVariable String status) {
		return jobCardService.findAllByJobStatus(status);
	}

	@PutMapping
	public ResponseEntity<?> update(@RequestBody JobCard jobCard) {
		try {
			return ResponseEntity.ok().body(jobCardService.update(jobCard));
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@PutMapping("/jobStatus")
	public ResponseEntity<?> updateJobStatus(@RequestBody JobCard jobCard) {
		try {
			return ResponseEntity.ok().body(jobCardService.updateJobStatus(jobCard));
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@GetMapping("/jobSpares/{id}")
	public JobSpares getJobSpares(@PathVariable String id) {
		return jobCardService.getJobSpares(id);
	}

	@PostMapping("/jobSpares")
	public ResponseEntity<?> updateJobSpares(@RequestBody JobSpares jobSparesInfo) {
		try {
			return ResponseEntity.ok().body(jobCardService.updateJobSpares(jobSparesInfo));
		} catch (Exception ex) {
			ex.printStackTrace();
			return ResponseEntity.badRequest().body(ex.getMessage());
		}
	}

	@GetMapping("/pdf/{id}")
	public ResponseEntity<?> generateJobCardPdf(@PathVariable String id) {

		try {
			return jobCardService.generateJobCardPdf(id);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}

	}
	
	@GetMapping("/billPdf/{id}")
	public ResponseEntity<?> generateBillPdf(@PathVariable String id) {

		try {
			return jobCardService.generateBillPdf(id);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}

	}
}
