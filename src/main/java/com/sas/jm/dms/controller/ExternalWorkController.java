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

import com.sas.jm.dms.entity.ExternalWorkCategory;
import com.sas.jm.dms.entity.ExternalWorkInventory;
import com.sas.jm.dms.model.ExternalWorkFilter;
import com.sas.jm.dms.service.ExternalWorkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/externalWork")
@RequiredArgsConstructor
@Slf4j
public class ExternalWorkController {

	private final ExternalWorkService externalWorkService;

	@GetMapping
	public List<?> findAll() {
		return externalWorkService.findAll();
	}

	@PostMapping
	public ExternalWorkInventory save(@RequestBody ExternalWorkInventory externalWork) {
		return externalWorkService.save(externalWork);
	}

	@PostMapping("/findExternalWorkInventoryWithFilter")
	public List<ExternalWorkInventory> findExternalWorkInventoryWithFilter(@RequestBody ExternalWorkFilter externalWorkFilter) {
		return externalWorkService.findExternalWorkInventoryWithFilter(externalWorkFilter);
	}

	@GetMapping("/externalWorkCategory")
	public List<?> findAllExternalWorkCategory() {
		return externalWorkService.findAllExternalWorkCategory();
	}

	@PostMapping("/saveExternalWorkCategory")
	public ResponseEntity<?> saveExternalWorkCategory(@RequestBody ExternalWorkCategory externalWorkCategory) {
		try {
			return ResponseEntity.ok().body(externalWorkService.saveExternalWorkCategory(externalWorkCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@DeleteMapping("/externalWorkCategory/{id}")
	public ResponseEntity<?> deleteExternalWorkCategory(@PathVariable String id) {
		try {
			return ResponseEntity.ok().body(externalWorkService.deleteExternalWorkCategoryById(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/externalWorkCategory/{oldCategory}/{newCategory}")
	public ResponseEntity<?> updateExternalWorkCategory(@PathVariable String oldCategory, @PathVariable String newCategory) {
		try {
			return ResponseEntity.ok().body(externalWorkService.updateExternalWorkCategory(oldCategory, newCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
