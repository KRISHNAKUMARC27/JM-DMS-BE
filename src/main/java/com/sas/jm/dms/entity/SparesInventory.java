package com.sas.jm.dms.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SparesInventory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String category;
	private String partNumber;
	private String desc;
	private BigDecimal purchaseRate;
	private BigDecimal qty;
	private BigDecimal sellRate;
	private BigDecimal amount;
	private BigDecimal minThresh;
	private LocalDateTime minThreshDate;
	private LocalDateTime updateDate;

	private String rack;
	private String misc1;
	private String misc2; // Stores Units type
	private String misc3;

	@Version
	private Long version;
}
