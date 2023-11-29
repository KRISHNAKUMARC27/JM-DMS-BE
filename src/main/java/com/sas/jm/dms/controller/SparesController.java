package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sas.jm.dms.entity.SparesCategory;
import com.sas.jm.dms.entity.SparesInventory;
import com.sas.jm.dms.model.SparesFilter;
import com.sas.jm.dms.service.SparesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/spares")
@RequiredArgsConstructor
@Slf4j
public class SparesController {

	private final SparesService sparesService;
	
	@GetMapping
	public List<?> findAll() {
		return sparesService.findAll();
	}
	
	@PostMapping
	public SparesInventory save(@RequestBody SparesInventory spares) {
		return sparesService.save(spares);
	}
	
	@PostMapping("/findSparesInventoryWithFilter")
	public List<SparesInventory> findSparesInventoryWithFilter(@RequestBody SparesFilter sparesFilter) {
		return sparesService.findSparesInventoryWithFilter(sparesFilter);
	}
	
	@GetMapping("/sparesCategory")
	public List<?> findAllSparesCategory() {
		return sparesService.findAllSparesCategory();
	}

	@PostMapping("/saveSparesCategory")
	public SparesCategory saveSparesCategory(@RequestBody SparesCategory sparesCategory) {
		return sparesService.saveSparesCategory(sparesCategory);
	}
}
