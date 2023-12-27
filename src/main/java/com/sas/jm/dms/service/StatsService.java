package com.sas.jm.dms.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.JobSpares;
import com.sas.jm.dms.entity.SparesEvents;
import com.sas.jm.dms.entity.SparesInventory;
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

	private Map<Integer, Integer> calculateYearlyJobCards(List<JobSpares> jobSparesList) {
		Map<Integer, Integer> yearlyJobCards = initializeYearMapJobCards();
		int currentYear = LocalDate.now().getYear();

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

	private List<Integer> getYearlyJobCardsSeries(List<JobSpares> jobSparesList) {
		Map<Integer, Integer> yearlyJobCards = calculateYearlyJobCards(jobSparesList);
		List<Integer> earningsSeries = new ArrayList<>();

		int currentMonth = LocalDate.now().getMonthValue();
		for (int month = 1; month <= 12; month++) {
			if (month <= currentMonth) {
				earningsSeries.add(yearlyJobCards.get(month));
			}
		}
		return earningsSeries;
	}

	public Map<String, Object> yearlyStatsJobCards() {
		List<JobSpares> currentSpares = getJobsClosedThisYear();
		List<Integer> jobCardSeries = getYearlyJobCardsSeries(currentSpares);

		Map<String, Object> currentValues = new HashMap<>();
		currentValues.put("jobCardSeries", jobCardSeries);
		currentValues.put("totalJobCards", currentSpares.size());

		Map<String, Object> chartData = getChartData();
		Map<String, Object> seriesItem = new HashMap<>();
		seriesItem.put("name", "Per month");
		seriesItem.put("data", jobCardSeries);

		chartData.put("series", List.of(seriesItem));

		Map<String, Object> options = (Map<String, Object>) chartData.get("options");
		options.put("yaxis", Map.of("min", 0, "max", 1000));

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

}
