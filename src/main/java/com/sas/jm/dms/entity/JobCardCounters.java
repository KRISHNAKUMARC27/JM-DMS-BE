package com.sas.jm.dms.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "JobCardCounters")
@Data
public class JobCardCounters {
	
	@Id
    private String id;
    private int sequenceValue;
    private int yearMonth;

}
