package com.sas.jm.dms.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SparesEvents implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String sparesId;
	private String notif;
	private LocalDateTime time;
}
