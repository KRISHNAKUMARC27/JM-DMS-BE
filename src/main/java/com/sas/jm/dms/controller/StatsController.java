package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sas.jm.dms.service.StatsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

	private final StatsService statsService;

	@GetMapping("/sparesEvents")
	public List<?> findAllSparesEvents() {
		return statsService.findAllSparesEvents();
	}

	@DeleteMapping("/sparesEvents/{id}")
	public ResponseEntity<?> deleteSparesEvents(@PathVariable String id) {
		try {
			return ResponseEntity.ok().body(statsService.deleteSparesEvents(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
