package com.sas.jm.dms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	public ResponseEntity<?> saveSparesCategory(@RequestBody SparesCategory sparesCategory) {
		try {
			return ResponseEntity.ok().body(sparesService.saveSparesCategory(sparesCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@DeleteMapping("/sparesCategory/{id}")
	public ResponseEntity<?> deleteSparesCategory(@PathVariable String id) {
		try {
			return ResponseEntity.ok().body(sparesService.deleteSparesCategoryById(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/sparesCategory/{oldCategory}/{newCategory}")
	public ResponseEntity<?> updateSparesCategory(@PathVariable String oldCategory, @PathVariable String newCategory) {
		try {
			return ResponseEntity.ok().body(sparesService.updateSparesCategory(oldCategory, newCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
