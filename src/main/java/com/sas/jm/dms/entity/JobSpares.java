package com.sas.jm.dms.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobSpares {
	
	@Id
	private String id;
	
	private Integer jobId;
	private LocalDateTime jobCloseDate;
	private List<JobSparesInfo> jobSparesInfo;
	private List<JobSparesInfo> jobLaborInfo;
	private BigDecimal totalSparesValue;
	private BigDecimal totalLabourValue;
	private BigDecimal grandTotal;

}
