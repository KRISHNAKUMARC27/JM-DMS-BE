package com.sas.jm.dms.entity;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsumablesInventory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String category;
	private String desc;

	//private BigDecimal amount;

}
