package com.sas.jm.dms.entity;

import java.io.Serializable;
import java.util.Set;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	private String username;
	private String password; // Store encrypted password
	private Set<Role> roles;
}
