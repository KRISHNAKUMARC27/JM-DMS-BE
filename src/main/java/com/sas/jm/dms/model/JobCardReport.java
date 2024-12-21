package com.sas.jm.dms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobCardReport {

	private Integer jobId;
	private Integer invoiceId;
	private String jobStatus;
	private LocalDateTime jobCloseDate;
	private String vehicleRegNo;
	@Builder.Default
	private BigDecimal totalSparesValue = BigDecimal.ZERO;

	@Builder.Default
	private BigDecimal totalConsumablesValue = BigDecimal.ZERO;

	@Builder.Default
	private BigDecimal totalLabourValue = BigDecimal.ZERO;

	@Builder.Default
	private BigDecimal totalExternalWorkValue = BigDecimal.ZERO;

	@Builder.Default
	private BigDecimal grandTotal = BigDecimal.ZERO;
}
