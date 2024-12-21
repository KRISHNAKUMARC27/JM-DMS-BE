package com.sas.jm.dms.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.JobCard;
import com.sas.jm.dms.entity.JobSpares;
import com.sas.jm.dms.entity.SparesEvents;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.model.JobCardReport;
import com.sas.jm.dms.repository.JobCardRepository;
import com.sas.jm.dms.repository.JobSparesRepository;
import com.sas.jm.dms.repository.SparesEventsRepository;
import com.sas.jm.dms.repository.SparesInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

	private final SparesEventsRepository sparesEventsRepository;
	private final SparesInventoryRepository sparesInventoryRepository;
	private final JobCardRepository jobCardRepository;
	private final JobSparesRepository jobSparesRepository;

//	List<BigDecimal> currentWeekEarningsSeries = null;
//	BigDecimal weekTotalSparesValueSum = BigDecimal.ZERO;
//	BigDecimal weekTotalLabourValueSum = BigDecimal.ZERO;
//	BigDecimal weekGrandTotalSum = BigDecimal.ZERO;

	// Map<String, Object> chartData = new HashMap<>();
	BigDecimal totalSparesWorth = BigDecimal.ZERO;

	@EventListener
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// getChartData();
		// weeklyStats();
//		dailyStats();
//		dailyStatsJobCards();
		List<SparesInventory> sparesList = sparesInventoryRepository.findAll();
		sparesList.forEach(s -> totalSparesWorth = totalSparesWorth.add(s.getAmount()));

	}

	public List<?> findAllSparesEvents() {
		return sparesEventsRepository.findAllByOrderByIdDesc();
	}

	public List<?> deleteSparesEvents(String id) throws Exception {
		SparesEvents events = sparesEventsRepository.findById(id)
				.orElseThrow(() -> new Exception("SparesEvents not found for id: " + id));
		SparesInventory spares = sparesInventoryRepository.findById(events.getSparesId())
				.orElseThrow(() -> new Exception("SparesInventory not found for id: " + events.getSparesId()));
		if (spares.getMinThresh().compareTo(spares.getQty()) > 0) {
			throw new Exception("Event cannot be deleted as spares is still less than the min threshold");
		}
		sparesEventsRepository.deleteById(id);
		return findAllSparesEvents();
	}

	private List<JobSpares> getJobsClosedToday() {
		LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
		LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
		return jobSparesRepository.findByJobCloseDateBetween(startOfToday, endOfToday);
	}

	private List<JobSpares> getJobsClosedThisWeek() {
		LocalDateTime startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.atStartOfDay();
		LocalDateTime endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(23, 59,
				59);
		return jobSparesRepository.findByJobCloseDateBetween(startOfWeek, endOfWeek);
	}

	private List<JobSpares> getJobsClosedThisMonth() {
		LocalDateTime startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
		LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);
		return jobSparesRepository.findByJobCloseDateBetween(startOfMonth, endOfMonth);
	}

	private List<JobSpares> getJobsClosedThisYear() {
		LocalDateTime startOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
		LocalDateTime endOfYear = LocalDate.now().with(TemporalAdjusters.lastDayOfYear()).atTime(23, 59, 59);
		return jobSparesRepository.findByJobCloseDateBetween(startOfYear, endOfYear);
	}

	private Map<DayOfWeek, BigDecimal> initializeWeekMap() {
		Map<DayOfWeek, BigDecimal> weekMap = new EnumMap<>(DayOfWeek.class);
		for (DayOfWeek day : DayOfWeek.values()) {
			weekMap.put(day, BigDecimal.ZERO);
		}
		return weekMap;
	}

	public Map<DayOfWeek, BigDecimal> calculateWeeklyEarnings(List<JobSpares> jobSparesList) {
		Map<DayOfWeek, BigDecimal> weeklyEarnings = initializeWeekMap();
		LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
		LocalDate endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY);

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null && !job.getGrandTotal().equals(BigDecimal.ZERO)) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (!jobDate.isBefore(startOfWeek) && !jobDate.isAfter(endOfWeek)) {
					DayOfWeek day = job.getJobCloseDate().getDayOfWeek();
					BigDecimal currentTotal = weeklyEarnings.get(day);
					weeklyEarnings.put(day, currentTotal.add(job.getGrandTotal()));
				}
			}
		}
		return weeklyEarnings;
	}

	public List<BigDecimal> getWeeklyEarningsSeries(List<JobSpares> jobSparesList) {
		Map<DayOfWeek, BigDecimal> weeklyEarnings = calculateWeeklyEarnings(jobSparesList);
		List<BigDecimal> earningsSeries = new ArrayList<>();

		for (DayOfWeek day : DayOfWeek.values()) {
			// Assuming the week starts on Monday
			if (day.getValue() <= LocalDate.now().getDayOfWeek().getValue()) {
				earningsSeries.add(weeklyEarnings.get(day));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> weeklyStats() {
		List<JobSpares> currentWeekSpares = getJobsClosedThisWeek();
		List<BigDecimal> currentWeekEarningsSeries = getWeeklyEarningsSeries(currentWeekSpares);
		// List<Integer> jobCardSeries = getWeeklyJobCardsSeries(currentWeekSpares);

		BigDecimal weekTotalSparesValueSum = BigDecimal.ZERO;
		BigDecimal weekTotalLabourValueSum = BigDecimal.ZERO;
		BigDecimal weekGrandTotalSum = BigDecimal.ZERO;

		for (JobSpares jobSpares : currentWeekSpares) {
			weekTotalSparesValueSum = weekTotalSparesValueSum
					.add(jobSpares.getTotalSparesValue() != null ? jobSpares.getTotalSparesValue() : BigDecimal.ZERO);
			weekTotalLabourValueSum = weekTotalLabourValueSum
					.add(jobSpares.getTotalLabourValue() != null ? jobSpares.getTotalLabourValue() : BigDecimal.ZERO);
			weekGrandTotalSum = weekGrandTotalSum
					.add(jobSpares.getGrandTotal() != null ? jobSpares.getGrandTotal() : BigDecimal.ZERO);
		}

		Map<String, Object> currentWeekValues = new HashMap<>();
		currentWeekValues.put("earningsSeries", currentWeekEarningsSeries);
		currentWeekValues.put("totalSparesValueSum", weekTotalSparesValueSum);
		currentWeekValues.put("totalLabourValueSum", weekTotalLabourValueSum);
		currentWeekValues.put("grandTotalSum", weekGrandTotalSum);

//		currentWeekValues.put("jobCardSeries", jobCardSeries);
//		currentWeekValues.put("totalJobCards", currentWeekSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per day");
		seriesItem.put("data", currentWeekEarningsSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("xaxis", Map.of("categories", List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")));
		options.put("yaxis", Map.of("min", 0, "max", 50000));

		currentWeekValues.put("chartData", chartData);

//		Map<String, Object> chartDataJobCard = getChartData();
//		Map<String, Object> seriesItemJobCard = new HashMap<>();
//		seriesItemJobCard.put("name", "Per day");
//		seriesItemJobCard.put("data", jobCardSeries);
//
//		chartDataJobCard.put("series", List.of(seriesItemJobCard));
//		
//		Map<String, Object> optionsJobCard = (Map<String, Object>) chartDataJobCard.get("options");
//		optionsJobCard.put("yaxis", Map.of("min", 0, "max", 50));

//		currentWeekValues.put("chartDataJobCard", chartDataJobCard);

		return currentWeekValues;
	}

	public List<BigDecimal> getTodayEarningsSeries(List<JobSpares> jobSparesList) {
		List<BigDecimal> earningsSeries = new ArrayList<>();

		for (JobSpares jobSpares : jobSparesList) {
			earningsSeries.add(jobSpares.getGrandTotal());
		}
		return earningsSeries;
	}

	public Map<String, Object> dailyStats() {
		List<JobSpares> currentDaySpares = getJobsClosedToday();
		List<BigDecimal> currentDayEarningsSeries = getTodayEarningsSeries(currentDaySpares);

		BigDecimal dayTotalSparesValueSum = BigDecimal.ZERO;
		BigDecimal dayTotalLabourValueSum = BigDecimal.ZERO;
		BigDecimal dayGrandTotalSum = BigDecimal.ZERO;

		for (JobSpares jobSpares : currentDaySpares) {
			dayTotalSparesValueSum = dayTotalSparesValueSum
					.add(jobSpares.getTotalSparesValue() != null ? jobSpares.getTotalSparesValue() : BigDecimal.ZERO);
			dayTotalLabourValueSum = dayTotalLabourValueSum
					.add(jobSpares.getTotalLabourValue() != null ? jobSpares.getTotalLabourValue() : BigDecimal.ZERO);
			dayGrandTotalSum = dayGrandTotalSum
					.add(jobSpares.getGrandTotal() != null ? jobSpares.getGrandTotal() : BigDecimal.ZERO);
		}

		Map<String, Object> currentDayValues = new HashMap<>();
		currentDayValues.put("earningsSeries", currentDayEarningsSeries);
		currentDayValues.put("totalSparesValueSum", dayTotalSparesValueSum);
		currentDayValues.put("totalLabourValueSum", dayTotalLabourValueSum);
		currentDayValues.put("grandTotalSum", dayGrandTotalSum);

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "job");
		seriesItem.put("data", currentDayEarningsSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 15000));

		currentDayValues.put("chartData", chartData);

		return currentDayValues;
	}

	private Map<Integer, BigDecimal> initializeMonthMap() {
		Map<Integer, BigDecimal> monthMap = new HashMap<>();
		for (int week = 1; week <= 5; week++) {
			monthMap.put(week, BigDecimal.ZERO);
		}
		return monthMap;
	}

	private Map<Integer, BigDecimal> calculateMonthlyEarnings(List<JobSpares> jobSparesList) {
		Map<Integer, BigDecimal> monthlyEarnings = initializeMonthMap();
		LocalDate startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
		LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
		WeekFields weekFields = WeekFields.of(Locale.getDefault());

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null && !job.getGrandTotal().equals(BigDecimal.ZERO)) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (!jobDate.isBefore(startOfMonth) && !jobDate.isAfter(endOfMonth)) {
					int weekOfMonth = jobDate.get(weekFields.weekOfMonth());
					BigDecimal currentTotal = monthlyEarnings.get(weekOfMonth);
					monthlyEarnings.put(weekOfMonth, currentTotal.add(job.getGrandTotal()));
				}
			}
		}
		return monthlyEarnings;
	}

	private List<BigDecimal> getMonthlyEarningsSeries(List<JobSpares> jobSparesList) {
		Map<Integer, BigDecimal> monthlyEarnings = calculateMonthlyEarnings(jobSparesList);
		List<BigDecimal> earningsSeries = new ArrayList<>();

		WeekFields weekFields = WeekFields.of(Locale.getDefault());
		int currentWeekOfMonth = LocalDate.now().get(weekFields.weekOfMonth());

		for (int week = 1; week <= 5; week++) {
			if (week <= currentWeekOfMonth) {
				earningsSeries.add(monthlyEarnings.get(week));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> monthlyStats() {
		List<JobSpares> currentMonthSpares = getJobsClosedThisMonth();
		List<BigDecimal> currentEarningsSeries = getMonthlyEarningsSeries(currentMonthSpares);

		BigDecimal totalSparesValueSum = BigDecimal.ZERO;
		BigDecimal totalLabourValueSum = BigDecimal.ZERO;
		BigDecimal grandTotalSum = BigDecimal.ZERO;

		for (JobSpares jobSpares : currentMonthSpares) {
			totalSparesValueSum = totalSparesValueSum
					.add(jobSpares.getTotalSparesValue() != null ? jobSpares.getTotalSparesValue() : BigDecimal.ZERO);
			totalLabourValueSum = totalLabourValueSum
					.add(jobSpares.getTotalLabourValue() != null ? jobSpares.getTotalLabourValue() : BigDecimal.ZERO);
			grandTotalSum = grandTotalSum
					.add(jobSpares.getGrandTotal() != null ? jobSpares.getGrandTotal() : BigDecimal.ZERO);
		}

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("earningsSeries", currentEarningsSeries);
		currentValues.put("totalSparesValueSum", totalSparesValueSum);
		currentValues.put("totalLabourValueSum", totalLabourValueSum);
		currentValues.put("grandTotalSum", grandTotalSum);

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per week");
		seriesItem.put("data", currentEarningsSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 100000));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private Map<Integer, BigDecimal> initializeYearMap() {
		Map<Integer, BigDecimal> yearMap = new HashMap<>();
		for (int month = 1; month <= 12; month++) {
			yearMap.put(month, BigDecimal.ZERO);
		}
		return yearMap;
	}

	private Map<Integer, BigDecimal> calculateYearlyEarnings(List<JobSpares> jobSparesList) {
		Map<Integer, BigDecimal> yearlyEarnings = initializeYearMap();
		int currentYear = LocalDate.now().getYear();

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null && !job.getGrandTotal().equals(BigDecimal.ZERO)) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (jobDate.getYear() == currentYear) {
					int month = jobDate.getMonthValue();
					BigDecimal currentTotal = yearlyEarnings.get(month);
					yearlyEarnings.put(month, currentTotal.add(job.getGrandTotal()));
				}
			}
		}
		return yearlyEarnings;
	}

	private List<BigDecimal> getYearlyEarningsSeries(List<JobSpares> jobSparesList) {
		Map<Integer, BigDecimal> yearlyEarnings = calculateYearlyEarnings(jobSparesList);
		List<BigDecimal> earningsSeries = new ArrayList<>();

		int currentMonth = LocalDate.now().getMonthValue();
		for (int month = 1; month <= 12; month++) {
			if (month <= currentMonth) {
				earningsSeries.add(yearlyEarnings.get(month));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> yearlyStats() {
		List<JobSpares> currentYearSpares = getJobsClosedThisYear();
		List<BigDecimal> currentEarningsSeries = getYearlyEarningsSeries(currentYearSpares);

		BigDecimal totalSparesValueSum = BigDecimal.ZERO;
		BigDecimal totalLabourValueSum = BigDecimal.ZERO;
		BigDecimal grandTotalSum = BigDecimal.ZERO;

		for (JobSpares jobSpares : currentYearSpares) {
			totalSparesValueSum = totalSparesValueSum
					.add(jobSpares.getTotalSparesValue() != null ? jobSpares.getTotalSparesValue() : BigDecimal.ZERO);
			totalLabourValueSum = totalLabourValueSum
					.add(jobSpares.getTotalLabourValue() != null ? jobSpares.getTotalLabourValue() : BigDecimal.ZERO);
			grandTotalSum = grandTotalSum
					.add(jobSpares.getGrandTotal() != null ? jobSpares.getGrandTotal() : BigDecimal.ZERO);
		}

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("earningsSeries", currentEarningsSeries);
		currentValues.put("totalSparesValueSum", totalSparesValueSum);
		currentValues.put("totalLabourValueSum", totalLabourValueSum);
		currentValues.put("grandTotalSum", grandTotalSum);

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per Month");
		seriesItem.put("data", currentEarningsSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 1000000));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private Map<DayOfWeek, Integer> initializeWeekMapJobCard() {
		Map<DayOfWeek, Integer> weekMap = new EnumMap<>(DayOfWeek.class);
		for (DayOfWeek day : DayOfWeek.values()) {
			weekMap.put(day, 0);
		}
		return weekMap;
	}

	public Map<DayOfWeek, Integer> calculateWeeklyJobCards(List<JobSpares> jobSparesList) {
		Map<DayOfWeek, Integer> weeklyJobs = initializeWeekMapJobCard();
		LocalDate startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY);
		LocalDate endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY);

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (!jobDate.isBefore(startOfWeek) && !jobDate.isAfter(endOfWeek)) {
					DayOfWeek day = job.getJobCloseDate().getDayOfWeek();
					Integer currentTotal = weeklyJobs.get(day);
					weeklyJobs.put(day, currentTotal + 1);
				}
			}
		}
		return weeklyJobs;
	}

	public List<Integer> getWeeklyJobCardsSeries(List<JobSpares> jobSparesList) {
		Map<DayOfWeek, Integer> weeklyJobs = calculateWeeklyJobCards(jobSparesList);
		List<Integer> earningsSeries = new ArrayList<>();

		for (DayOfWeek day : DayOfWeek.values()) {
			// Assuming the week starts on Monday
			if (day.getValue() <= LocalDate.now().getDayOfWeek().getValue()) {
				earningsSeries.add(weeklyJobs.get(day));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> weeklyStatsJobCards() {
		List<JobSpares> currentSpares = getJobsClosedThisWeek();
		List<Integer> jobCardSeries = getWeeklyJobCardsSeries(currentSpares);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("jobCardSeries", jobCardSeries);
		currentValues.put("totalJobCards", currentSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per day");
		seriesItem.put("data", jobCardSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 50));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	public List<Integer> getTodayJobCardsSeries(List<JobSpares> jobSparesList) {
		List<Integer> earningsSeries = new ArrayList<>();

		for (JobSpares jobSpares : jobSparesList) {
			earningsSeries.add(1);
		}
		return earningsSeries;
	}

	public Map<String, Object> dailyStatsJobCards() {
		List<JobSpares> currentSpares = getJobsClosedToday();
		List<Integer> jobCardSeries = getTodayJobCardsSeries(currentSpares);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("jobCardSeries", jobCardSeries);
		currentValues.put("totalJobCards", currentSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Job");
		seriesItem.put("data", jobCardSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 20));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private Map<Integer, Integer> initializeMonthMapJobCards() {
		Map<Integer, Integer> monthMap = new HashMap<>();
		for (int week = 1; week <= 5; week++) {
			monthMap.put(week, 0);
		}
		return monthMap;
	}

	private Map<Integer, Integer> calculateMonthlyJobCards(List<JobSpares> jobSparesList) {
		Map<Integer, Integer> monthlyJobCards = initializeMonthMapJobCards();
		LocalDate startOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
		LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
		WeekFields weekFields = WeekFields.of(Locale.getDefault());

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (!jobDate.isBefore(startOfMonth) && !jobDate.isAfter(endOfMonth)) {
					int weekOfMonth = jobDate.get(weekFields.weekOfMonth());
					Integer currentTotal = monthlyJobCards.get(weekOfMonth);
					monthlyJobCards.put(weekOfMonth, currentTotal + 1);
				}
			}
		}
		return monthlyJobCards;
	}

	private List<Integer> getMonthlyJobCardsSeries(List<JobSpares> jobSparesList) {
		Map<Integer, Integer> monthlyJobCards = calculateMonthlyJobCards(jobSparesList);
		List<Integer> earningsSeries = new ArrayList<>();

		WeekFields weekFields = WeekFields.of(Locale.getDefault());
		int currentWeekOfMonth = LocalDate.now().get(weekFields.weekOfMonth());

		for (int week = 1; week <= 5; week++) {
			if (week <= currentWeekOfMonth) {
				earningsSeries.add(monthlyJobCards.get(week));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> monthlyStatsJobCards() {
		List<JobSpares> currentSpares = getJobsClosedThisMonth();
		List<Integer> jobCardSeries = getMonthlyJobCardsSeries(currentSpares);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("jobCardSeries", jobCardSeries);
		currentValues.put("totalJobCards", currentSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per week");
		seriesItem.put("data", jobCardSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 100));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private Map<Integer, Integer> initializeYearMapJobCards() {
		Map<Integer, Integer> yearMap = new HashMap<>();
		for (int month = 1; month <= 12; month++) {
			yearMap.put(month, 0);
		}
		return yearMap;
	}

	private Map<Integer, BigDecimal> initializeYearMapJobCardsEarning() {
		Map<Integer, BigDecimal> yearMap = new HashMap<>();
		for (int month = 1; month <= 12; month++) {
			yearMap.put(month, BigDecimal.ZERO);
		}
		return yearMap;
	}

	private Map<Integer, Integer> calculateYearlyJobCards(List<JobSpares> jobSparesList, String year) {
		Map<Integer, Integer> yearlyJobCards = initializeYearMapJobCards();
		int currentYear = LocalDate.now().getYear();
		if (year != null)
			currentYear = Integer.parseInt(year);

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (jobDate.getYear() == currentYear) {
					int month = jobDate.getMonthValue();
					Integer currentTotal = yearlyJobCards.get(month);
					yearlyJobCards.put(month, currentTotal + 1);
				}
			}
		}
		return yearlyJobCards;
	}

	private List<Integer> getYearlyJobCardsSeries(List<JobSpares> jobSparesList, String year) {
		Map<Integer, Integer> yearlyJobCards = calculateYearlyJobCards(jobSparesList, year);
		List<Integer> earningsSeries = new ArrayList<>();

		// int currentMonth = LocalDate.now().getMonthValue();
		for (int month = 1; month <= 12; month++) {
			// if (month <= currentMonth) {
			earningsSeries.add(yearlyJobCards.get(month));
		}
		// }
		return earningsSeries;
	}

	public Map<String, Object> yearlyStatsJobCards() {
		List<JobSpares> currentSpares = getJobsClosedThisYear();
		List<Integer> jobCardSeries = getYearlyJobCardsSeries(currentSpares, null);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("jobCardSeries", jobCardSeries);
		currentValues.put("totalJobCards", currentSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per month");
		seriesItem.put("data", jobCardSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 250));

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	public Map<String, Object> getChartData() {
		Map<String, Object> chartData = new HashMap<>();

		chartData.put("type", "line");
		chartData.put("height", 90);

		Map<String, Object> options = new HashMap<>();
		Map<String, Object> chart = new HashMap<>();
		Map<String, Object> sparkline = new HashMap<>();
		sparkline.put("enabled", true);
		chart.put("sparkline", sparkline);
		options.put("chart", chart);

		options.put("dataLabels", Map.of("enabled", false));
		options.put("colors", List.of("#fff"));

		Map<String, Object> fill = new HashMap<>();
		fill.put("type", "solid");
		fill.put("opacity", 1);
		options.put("fill", fill);

		Map<String, Object> stroke = new HashMap<>();
		stroke.put("curve", "smooth");
		stroke.put("width", 3);
		options.put("stroke", stroke);

		options.put("yaxis", Map.of("min", 0, "max", 100));

		Map<String, Object> tooltip = new HashMap<>();
		tooltip.put("theme", "dark");
		tooltip.put("fixed", Map.of("enabled", false));
		tooltip.put("x", Map.of("show", false));
		tooltip.put("y", Map.of("title", "Total Order"));
		tooltip.put("marker", Map.of("show", false));
		options.put("tooltip", tooltip);

		chartData.put("options", options);

		return chartData;
	}

	public Map<String, BigDecimal> getTotalRevenue() {
		Map<String, BigDecimal> revenueMap = new HashMap<>();

		BigDecimal totalRevenue = BigDecimal.ZERO;
		BigDecimal totalLabourValueSum = BigDecimal.ZERO;
		BigDecimal totalSparesSum = BigDecimal.ZERO;
		List<JobSpares> allJobsSpares = jobSparesRepository.findByJobCloseDateNotNull();
		for (JobSpares job : allJobsSpares) {
			if (job.getJobCloseDate() != null && !job.getGrandTotal().equals(BigDecimal.ZERO)) {
				totalRevenue = totalRevenue.add(job.getGrandTotal());
			}
			if (job.getJobCloseDate() != null && !job.getTotalLabourValue().equals(BigDecimal.ZERO)) {
				totalLabourValueSum = totalLabourValueSum.add(job.getTotalLabourValue());
			}
			if (job.getJobCloseDate() != null && !job.getTotalSparesValue().equals(BigDecimal.ZERO)) {
				totalSparesSum = totalSparesSum.add(job.getTotalSparesValue());
			}
		}
		revenueMap.put("total", totalRevenue);
		revenueMap.put("labor", totalLabourValueSum);
		revenueMap.put("spares", totalSparesSum);
		revenueMap.put("totalSparesWorth", totalSparesWorth);
		return revenueMap;
	}

	public Map<String, Long> getTotalJobCards() {
		Map<String, Long> countMap = new HashMap<>();
		countMap.put("total", jobCardRepository.count());
		countMap.put("closed", jobCardRepository.countByJobStatus("CLOSED"));
		countMap.put("open", jobCardRepository.countByJobStatus("OPEN"));
		countMap.put("cancelled", jobCardRepository.countByJobStatus("CANCELLED"));

		return countMap;
	}

	public Map<String, Object> yearlyBarStatsJobCards(String year) {

		List<JobSpares> currentSpares = getJobsClosedForTheYear(year);

		List<Integer> jobCardSeries = getYearlyJobCardsSeries(currentSpares, year);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("totalJobCard", currentSpares.size());

		Map<String, Object> chartData = getBarChartData();

		List<Map<String, Object>> series = Arrays.asList(createSeries("Completed", jobCardSeries));

		chartData.put("series", series);

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private List<JobSpares> getJobsClosedForTheYear(String year) {
		// Parse the year parameter to an integer
		int targetYear = Integer.parseInt(year);

		// Calculate startOfYear and endOfYear based on the target year
		LocalDateTime startOfYear = LocalDate.of(targetYear, 1, 1).atStartOfDay();
		LocalDateTime endOfYear = LocalDate.of(targetYear, 12, 31).atTime(23, 59, 59);

		// Use the calculated dates to fetch data
		return jobSparesRepository.findByJobCloseDateBetween(startOfYear, endOfYear);
	}

	public Map<String, Object> getBarChartData() {
		Map<String, Object> chartData = new HashMap<>();

		chartData.put("height", 480);
		chartData.put("type", "bar");

		// options
		Map<String, Object> options = new HashMap<>();
		Map<String, Object> chart = new HashMap<>();
		chart.put("id", "bar-chart");
		chart.put("stacked", true);

		Map<String, Object> toolbar = new HashMap<>();
		toolbar.put("show", true);
		chart.put("toolbar", toolbar);

		Map<String, Object> zoom = new HashMap<>();
		zoom.put("enabled", true);
		chart.put("zoom", zoom);
		options.put("chart", chart);

		// responsive
		Map<String, Object> responsiveOption = new HashMap<>();
		responsiveOption.put("breakpoint", 480);

		Map<String, Object> responsiveOptionNested = new HashMap<>();
		Map<String, Object> legendNested = new HashMap<>();
		legendNested.put("position", "bottom");
		legendNested.put("offsetX", -10);
		legendNested.put("offsetY", 0);
		responsiveOptionNested.put("legend", legendNested);

		responsiveOption.put("options", responsiveOptionNested);
		options.put("responsive", Arrays.asList(responsiveOption));

		// plotOptions
		Map<String, Object> plotOptions = new HashMap<>();
		Map<String, Object> bar = new HashMap<>();
		bar.put("horizontal", false);
		bar.put("columnWidth", "50%");
		plotOptions.put("bar", bar);
		options.put("plotOptions", plotOptions);

		// xaxis
		Map<String, Object> xaxis = new HashMap<>();
		xaxis.put("type", "category");
		xaxis.put("categories",
				Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));
		options.put("xaxis", xaxis);

		// legend
		Map<String, Object> legend = new HashMap<>();
		legend.put("show", true);
		legend.put("fontSize", "14px");
		legend.put("fontFamily", "'Roboto', sans-serif");
		legend.put("position", "bottom");
		legend.put("offsetX", 20);

		Map<String, Object> labels = new HashMap<>();
		labels.put("useSeriesColors", false);
		legend.put("labels", labels);

		Map<String, Object> markers = new HashMap<>();
		markers.put("width", 16);
		markers.put("height", 16);
		markers.put("radius", 5);
		legend.put("markers", markers);

		Map<String, Object> itemMargin = new HashMap<>();
		itemMargin.put("horizontal", 15);
		itemMargin.put("vertical", 8);
		legend.put("itemMargin", itemMargin);
		options.put("legend", legend);

		// fill
		Map<String, Object> fill = new HashMap<>();
		fill.put("type", "solid");
		options.put("fill", fill);

		// dataLabels
		Map<String, Object> dataLabels = new HashMap<>();
		dataLabels.put("enabled", false);
		options.put("dataLabels", dataLabels);

		// grid
		Map<String, Object> grid = new HashMap<>();
		grid.put("show", true);
		options.put("grid", grid);

		chartData.put("options", options);

		// series
		List<Map<String, Object>> series = Arrays.asList(
				createSeries("Investment", Arrays.asList(35, 125, 35, 35, 35, 80, 35, 20, 35, 45, 15, 75)),
				createSeries("Loss", Arrays.asList(35, 15, 15, 35, 65, 40, 80, 25, 15, 85, 25, 75)),
				createSeries("Profit", Arrays.asList(35, 145, 35, 35, 20, 105, 100, 10, 65, 45, 30, 10)),
				createSeries("Maintenance", Arrays.asList(0, 0, 75, 0, 0, 115, 0, 0, 0, 0, 150, 0)));

		chartData.put("series", series);
		return chartData;
	}

	private static Map<String, Object> createSeries(String name, List<Integer> data) {
		Map<String, Object> series = new HashMap<>();
		series.put("name", name);
		series.put("data", data);
		return series;
	}

	private static Map<String, Object> createSeriesEarning(String name, List<BigDecimal> data) {
		Map<String, Object> series = new HashMap<>();
		series.put("name", name);
		series.put("data", data);
		return series;
	}

	public Map<String, Object> yearlyBarStatsTotalRevenue(String year) {

		List<JobSpares> currentSpares = getJobsClosedForTheYear(year);

		List<BigDecimal> jobCardEarningSeries = getYearlyJobCardEarningSeries(currentSpares, year);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("totalJobCard", currentSpares.size());

		Map<String, Object> chartData = getBarChartData();

		List<Map<String, Object>> series = Arrays.asList(createSeriesEarning("Completed", jobCardEarningSeries));

		chartData.put("series", series);

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private List<BigDecimal> getYearlyJobCardEarningSeries(List<JobSpares> jobSparesList, String year) {
		Map<Integer, BigDecimal> yearlyJobCardsEarning = calculateYearlyJobCardsEarning(jobSparesList, year);
		List<BigDecimal> earningsSeries = new ArrayList<>();

		// int currentMonth = LocalDate.now().getMonthValue();
		for (int month = 1; month <= 12; month++) {
			// if (month <= currentMonth) {
			earningsSeries.add(yearlyJobCardsEarning.get(month));
		}
		// }
		return earningsSeries;
	}

	private Map<Integer, BigDecimal> calculateYearlyJobCardsEarning(List<JobSpares> jobSparesList, String year) {
		Map<Integer, BigDecimal> yearlyJobCards = initializeYearMapJobCardsEarning();
		int currentYear = LocalDate.now().getYear();
		if (year != null)
			currentYear = Integer.parseInt(year);

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (jobDate.getYear() == currentYear) {
					int month = jobDate.getMonthValue();
					BigDecimal currentTotal = yearlyJobCards.get(month);
					yearlyJobCards.put(month, currentTotal.add(job.getGrandTotal()));
				}
			}
		}
		return yearlyJobCards;
	}

	public Map<String, Object> yearlyStatsEarningSplit(String year) {
		List<JobSpares> currentSpares = getJobsClosedForTheYear(year);
		List<Map<String, BigDecimal>> jobSparesSeries = getYearlyEarningSplitSeries(currentSpares, year);

		List<BigDecimal> sparesSeries = new ArrayList<>();
		List<BigDecimal> laborSeries = new ArrayList<>();
		List<BigDecimal> consumablesSeries = new ArrayList<>();
		List<BigDecimal> externalWorkSeries = new ArrayList<>();

		for (Map<String, BigDecimal> map : jobSparesSeries) {
			sparesSeries.add(map.get("SPARES"));
			laborSeries.add(map.get("LABOR"));
			consumablesSeries.add(map.get("CONSUMABLES"));
			externalWorkSeries.add(map.get("EXTERNALWORK"));
		}

		BigDecimal totalGrandTotal = currentSpares.stream().map(JobSpares::getGrandTotal) // Extract the grandTotal
																							// field from each JobSpares
																							// object
				.filter(Objects::nonNull) // Filter out any null values to avoid NullPointerException
				.reduce(BigDecimal.ZERO, BigDecimal::add); // Sum up all the values

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("totalRevenue", totalGrandTotal);

		Map<String, Object> chartData = getBarChartData();

		List<Map<String, Object>> series = Arrays.asList(createSeriesEarning("SPARES", sparesSeries),
				createSeriesEarning("LABOR", laborSeries), createSeriesEarning("CONSUMABLES", consumablesSeries),
				createSeriesEarning("EXTERNALWORK", externalWorkSeries));

		chartData.put("series", series);

		currentValues.put("chartData", chartData);

		return currentValues;
	}

	private List<Map<String, BigDecimal>> getYearlyEarningSplitSeries(List<JobSpares> jobSparesList, String year) {
		Map<Integer, Map<String, BigDecimal>> yearlyJobCardsEarning = calculateYearlyEarningSplit(jobSparesList, year);
		List<Map<String, BigDecimal>> earningsSeries = new ArrayList<>();

		// int currentMonth = LocalDate.now().getMonthValue();
		for (int month = 1; month <= 12; month++) {
			// if (month <= currentMonth) {
			earningsSeries.add(yearlyJobCardsEarning.get(month));
		}
		// }
		return earningsSeries;
	}

	private Map<Integer, Map<String, BigDecimal>> calculateYearlyEarningSplit(List<JobSpares> jobSparesList,
			String year) {
		Map<Integer, Map<String, BigDecimal>> yearlyJobCards = initializeYearMapEarningSplit();
		int currentYear = LocalDate.now().getYear();
		if (year != null)
			currentYear = Integer.parseInt(year);

		for (JobSpares job : jobSparesList) {
			if (job.getJobCloseDate() != null) {
				LocalDate jobDate = job.getJobCloseDate().toLocalDate();
				if (jobDate.getYear() == currentYear) {
					int month = jobDate.getMonthValue();

					Map<String, BigDecimal> currentTotal = yearlyJobCards.get(month);
					currentTotal.put("SPARES",
							currentTotal.getOrDefault("SPARES", BigDecimal.ZERO).add(job.getTotalSparesValue()));
					currentTotal.put("LABOR",
							currentTotal.getOrDefault("LABOR", BigDecimal.ZERO).add(job.getTotalLabourValue()));
					currentTotal.put("CONSUMABLES", currentTotal.getOrDefault("CONSUMABLES", BigDecimal.ZERO).add(
							job.getTotalConsumablesValue() != null ? job.getTotalConsumablesValue() : BigDecimal.ZERO));
					currentTotal.put("EXTERNALWORK",
							currentTotal.getOrDefault("EXTERNALWORK", BigDecimal.ZERO)
									.add(job.getTotalExternalWorkValue() != null ? job.getTotalExternalWorkValue()
											: BigDecimal.ZERO));

					yearlyJobCards.put(month, currentTotal);
				}
			}
		}
		return yearlyJobCards;
	}

	private Map<Integer, Map<String, BigDecimal>> initializeYearMapEarningSplit() {
		Map<Integer, Map<String, BigDecimal>> yearMap = new HashMap<>();
		for (int month = 1; month <= 12; month++) {
			Map<String, BigDecimal> counts = new HashMap<>();
			yearMap.put(month, counts);
		}
		return yearMap;
	}

	public List<JobCardReport> getJobSparesByMonthAndYear(int month, int year) {
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
		LocalDateTime endDate = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
		List<JobSpares> jobSparesList = jobSparesRepository.findByJobCloseDateBetween(startDate, endDate);

		Map<String, JobSpares> jobSparesMap = new HashMap<>();
		jobSparesList.stream().forEach(jobSpares -> {
			jobSparesMap.put(jobSpares.getId(), jobSpares);
		});
		List<JobCard> jobCardList = jobCardRepository.findByJobCloseDateBetweenAndJobStatusOrderByJobIdDesc(startDate,
				endDate, "CLOSED");

		List<JobCardReport> jobCardReportList = new ArrayList<>();
		for (JobCard jobCard : jobCardList) {
			JobSpares jobSpares = jobSparesMap.get(jobCard.getId());
			JobCardReport jobCardReport = null;
			if (jobSpares != null) {
				jobCardReport = JobCardReport.builder().jobId(jobCard.getJobId()).invoiceId(jobCard.getInvoiceId())
						.jobStatus(jobCard.getJobStatus()).jobCloseDate(jobCard.getJobCloseDate())
						.vehicleRegNo(jobCard.getVehicleRegNo()).totalSparesValue(jobSpares.getTotalSparesValue())
						.totalConsumablesValue(jobSpares.getTotalConsumablesValue() != null ? jobSpares.getTotalConsumablesValue() : BigDecimal.ZERO)
						.totalExternalWorkValue(jobSpares.getTotalExternalWorkValue() != null ? jobSpares.getTotalExternalWorkValue() : BigDecimal.ZERO)
						.totalLabourValue(jobSpares.getTotalLabourValue() != null ? jobSpares.getTotalLabourValue() : BigDecimal.ZERO)
						.grandTotal(jobSpares.getGrandTotal() != null ? jobSpares.getGrandTotal() : BigDecimal.ZERO)
						.build();
			} else {
				jobCardReport = JobCardReport.builder().jobId(jobCard.getJobId()).invoiceId(jobCard.getInvoiceId())
						.jobStatus(jobCard.getJobStatus()).jobCloseDate(jobCard.getJobCloseDate())
						.vehicleRegNo(jobCard.getVehicleRegNo()).build();
			}
			jobCardReportList.add(jobCardReport);
		}
		
		return jobCardReportList;
	}

}
