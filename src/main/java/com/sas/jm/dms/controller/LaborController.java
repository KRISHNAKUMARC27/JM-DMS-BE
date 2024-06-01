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

import com.sas.jm.dms.entity.LaborCategory;
import com.sas.jm.dms.entity.LaborInventory;
import com.sas.jm.dms.model.LaborFilter;
import com.sas.jm.dms.service.LaborService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/labor")
@RequiredArgsConstructor
@Slf4j
public class LaborController {

	private final LaborService laborService;

	@GetMapping
	public List<?> findAll() {
		return laborService.findAll();
	}

	@PostMapping
	public LaborInventory save(@RequestBody LaborInventory labor) {
		return laborService.save(labor);
	}

	@PostMapping("/findLaborInventoryWithFilter")
	public List<LaborInventory> findLaborInventoryWithFilter(@RequestBody LaborFilter laborFilter) {
		return laborService.findLaborInventoryWithFilter(laborFilter);
	}

	@GetMapping("/laborCategory")
	public List<?> findAllLaborCategory() {
		return laborService.findAllLaborCategory();
	}

	@PostMapping("/saveLaborCategory")
	public ResponseEntity<?> saveLaborCategory(@RequestBody LaborCategory laborCategory) {
		try {
			return ResponseEntity.ok().body(laborService.saveLaborCategory(laborCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@DeleteMapping("/laborCategory/{id}")
	public ResponseEntity<?> deleteLaborCategory(@PathVariable String id) {
		try {
			return ResponseEntity.ok().body(laborService.deleteLaborCategoryById(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/laborCategory/{oldCategory}/{newCategory}")
	public ResponseEntity<?> updateLaborCategory(@PathVariable String oldCategory, @PathVariable String newCategory) {
		try {
			return ResponseEntity.ok().body(laborService.updateLaborCategory(oldCategory, newCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
