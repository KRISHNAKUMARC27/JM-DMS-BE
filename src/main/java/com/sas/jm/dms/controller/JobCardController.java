package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sas.jm.dms.entity.JobCard;
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
}
