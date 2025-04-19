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

import com.sas.jm.dms.entity.ConsumablesCategory;
import com.sas.jm.dms.entity.ConsumablesInventory;
import com.sas.jm.dms.model.ConsumablesFilter;
import com.sas.jm.dms.service.ConsumablesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/consumables")
@RequiredArgsConstructor
@Slf4j
public class ConsumablesController {

	private final ConsumablesService consumablesService;

	@GetMapping
	public List<?> findAll() {
		return consumablesService.findAll();
	}

	@PostMapping
	public ConsumablesInventory save(@RequestBody ConsumablesInventory consumables) {
		return consumablesService.save(consumables);
	}

	@PostMapping("/findConsumablesInventoryWithFilter")
	public List<ConsumablesInventory> findConsumablesInventoryWithFilter(@RequestBody ConsumablesFilter consumablesFilter) {
		return consumablesService.findConsumablesInventoryWithFilter(consumablesFilter);
	}

	@GetMapping("/consumablesCategory")
	public List<?> findAllConsumablesCategory() {
		return consumablesService.findAllConsumablesCategory();
	}

	@PostMapping("/saveConsumablesCategory")
	public ResponseEntity<?> saveConsumablesCategory(@RequestBody ConsumablesCategory consumablesCategory) {
		try {
			return ResponseEntity.ok().body(consumablesService.saveConsumablesCategory(consumablesCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@DeleteMapping("/consumablesCategory/{id}")
	public ResponseEntity<?> deleteConsumablesCategory(@PathVariable String id) {
		try {
			return ResponseEntity.ok().body(consumablesService.deleteConsumablesCategoryById(id));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/consumablesCategory/{oldCategory}/{newCategory}")
	public ResponseEntity<?> updateConsumablesCategory(@PathVariable String oldCategory, @PathVariable String newCategory) {
		try {
			return ResponseEntity.ok().body(consumablesService.updateConsumablesCategory(oldCategory, newCategory));
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
