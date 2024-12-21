//package com.sas.jm.dms;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Service;
//
//import com.sas.jm.dms.entity.JobCard;
//import com.sas.jm.dms.entity.JobSpares;
//import com.sas.jm.dms.repository.JobCardRepository;
//import com.sas.jm.dms.repository.JobSparesRepository;
//
//import lombok.AllArgsConstructor;
//
//@Service
//@AllArgsConstructor
//public class TestClass {
//
//	private final JobCardRepository jobCardRepository;
//	private final JobSparesRepository jobSparesRepository;
//
//	@EventListener
//	public void onApplicationEvent(ContextRefreshedEvent event) {
//
//		List<JobCard> jobCardList = jobCardRepository.findAll();
//		int jobCardCount = 0;
//		Set<String> jobCardSet = new HashSet<>();
//		for (JobCard jobCard : jobCardList) {
//			if (String.valueOf(jobCard.getJobCloseDate()).contains("2024-12")) {
//				jobCardCount++;
//				jobCardSet.add(jobCard.getId());
//			}
//		}
//
//		System.out.println("jobCardCount count of Novemeber 2024 " + jobCardSet.size());
//
//		List<JobSpares> jobSparesList = jobSparesRepository.findAll();
//		Set<String> jobSparesSet = new HashSet<>();
//		int jobSparesCount = 0;
//		for (JobSpares jobSpares : jobSparesList) {
//			if (String.valueOf(jobSpares.getJobCloseDate()).contains("2024-12")) {
//				jobSparesCount++;
//				jobSparesSet.add(jobSpares.getId());
//			}
//		}
//
//		System.out.println("jobSparesCount count of Novemeber 2024 " + jobSparesSet.size());
//
//		jobCardSet.removeAll(jobSparesSet);
//
//		System.out.println("Missing size " + jobCardSet.size());
//
//		for (String id : jobCardSet) {
//			JobCard jc = jobCardRepository.findById(id).orElse(null);
//			JobSpares js = jobSparesRepository.findById(id).orElse(null);
//			System.out.println("Card " + id + "    " + jc.getInvoiceId() + "    " + jc.getJobCloseDate());
//			if (js == null) {
//				System.out.println("Null JS " + jc.getVehicleRegNo());
//			} else {
//				System.out.println("JobSpares " + id + "    " + js.getJobCloseDate());
////				js.setJobCloseDate(jc.getJobCloseDate());
////				jobSparesRepository.save(js);
//			}
//		}
//
//	}
//
//}
