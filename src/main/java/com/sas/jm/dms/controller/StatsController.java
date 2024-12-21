package com.sas.jm.dms.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sas.jm.dms.model.JobCardReport;
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

	@GetMapping("/revenueEarnings/{id}")
	public Map<String, Object> getCurrentWeekEarnings(@PathVariable String id) {
		switch (id) {
		case "D":
			return statsService.dailyStats();
		case "W":
			return statsService.weeklyStats();
		case "M":
			return statsService.monthlyStats();
		case "Y":
			return statsService.yearlyStats();
		default:
			return statsService.dailyStats();
		}
	}

	@GetMapping("/totalJobCards/{id}")
	public Map<String, Object> getTotalJobCards(@PathVariable String id) {
		switch (id) {
		case "D":
			return statsService.dailyStatsJobCards();
		case "W":
			return statsService.weeklyStatsJobCards();
		case "M":
			return statsService.monthlyStatsJobCards();
		case "Y":
			return statsService.yearlyStatsJobCards();
		default:
			return statsService.dailyStatsJobCards();
		}
	}

	@GetMapping("/yearlyBarStatsJobCards/{id}")
	public Map<String, Object> yearlyBarStatsJobCards(@PathVariable String id) {
		return statsService.yearlyBarStatsJobCards(id);
	}
	
	@GetMapping("/yearlyBarStatsTotalRevenue/{id}")
	public Map<String, Object> yearlyBarStatsTotalRevenue(@PathVariable String id) {
		return statsService.yearlyBarStatsTotalRevenue(id);
	}
	
	@GetMapping("/yearlyStatsEarningSplit/{id}")
	public Map<String, Object> yearlyStatsEarningSplit(@PathVariable String id) {
		return statsService.yearlyStatsEarningSplit(id);
	}

	@GetMapping("/totalRevenue")
	public Map<String, BigDecimal> getTotalRevenue() {
		return statsService.getTotalRevenue();
	}

	@GetMapping("/totalJobCards")
	public Map<String, Long> getTotalJobCards() {
		return statsService.getTotalJobCards();
	}

	@GetMapping("/jobCardReport/{month}/{year}")
	public List<JobCardReport> getJobSparesByMonthAndYear(@PathVariable String month, @PathVariable String year) {
		return statsService.getJobSparesByMonthAndYear(Integer.valueOf(month), Integer.valueOf(year));
	}
}
