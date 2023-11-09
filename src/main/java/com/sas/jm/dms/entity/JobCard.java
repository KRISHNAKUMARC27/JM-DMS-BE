package com.sas.jm.dms.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobCard implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	
	private Integer jobId;
	private String jobStatus;
	private LocalDateTime jobCreationDate;
	private String ownerName;
	private String ownerAddress;
	private String ownerPhoneNumber;
	private String vehicleRegNo;
	private String vehicleName;
	private String vehicleModel;
	private Integer kiloMeters;
	private String technicianName;
	private LocalDateTime vehicleOutDate;
	private List<JobCardInfo> jobInfo;

}
