package com.sas.jm.dms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.ConsumablesCategory;
import com.sas.jm.dms.entity.ConsumablesInventory;
import com.sas.jm.dms.model.ConsumablesFilter;
import com.sas.jm.dms.repository.ConsumablesCategoryRepository;
import com.sas.jm.dms.repository.ConsumablesInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsumablesService {

	private final ConsumablesCategoryRepository consumablesCategoryRepository;
	private final ConsumablesInventoryRepository consumablesInventoryRepository;
	
	public List<?> findAll() {
		return consumablesInventoryRepository.findAllByOrderByIdDesc();
	}
	
	public ConsumablesInventory findById(String id) throws Exception {
		return consumablesInventoryRepository.findById(id).orElse(null);
	}
	
	public ConsumablesInventory save(ConsumablesInventory consumables) {
		
		String oldConsumablesCategory = null;
		if (consumables.getId() != null) {
			// the consumables is getting updated.
			ConsumablesInventory oldConsumables = consumablesInventoryRepository.findById(consumables.getId()).orElse(null);
			if (oldConsumables != null && !oldConsumables.getCategory().equals(consumables.getCategory())) {
				oldConsumablesCategory = oldConsumables.getCategory();
			}
		}
		
		consumables = consumablesInventoryRepository.save(consumables);
		
		Integer consumablesCategoryCount = consumablesInventoryRepository.countByCategory(consumables.getCategory());
		ConsumablesCategory consumablesCategory = consumablesCategoryRepository.findByCategory(consumables.getCategory());
		consumablesCategory.setConsumablesCount(consumablesCategoryCount);
		consumablesCategoryRepository.save(consumablesCategory);

		if (oldConsumablesCategory != null) {
			Integer oldConsumablesCategoryCount = consumablesInventoryRepository.countByCategory(oldConsumablesCategory);
			ConsumablesCategory oldConsumablesCat = consumablesCategoryRepository.findByCategory(oldConsumablesCategory);
			oldConsumablesCat.setConsumablesCount(oldConsumablesCategoryCount);
			consumablesCategoryRepository.save(oldConsumablesCat);
		}
		return consumablesInventoryRepository.save(consumables);
	}
	
	public List<ConsumablesInventory> findConsumablesInventoryWithFilter(ConsumablesFilter consumablesFilter) {
		return consumablesInventoryRepository.findConsumablesInventoryWithFilter(consumablesFilter.categoryList(),
				consumablesFilter.desc());
	}

	public List<?> findAllConsumablesCategory() {
		return consumablesCategoryRepository.findAll();
	}

	public ConsumablesCategory saveConsumablesCategory(ConsumablesCategory consumablesCategory) throws Exception {
		ConsumablesCategory category = consumablesCategoryRepository.findByCategory(consumablesCategory.getCategory());
		if (category == null)
			return consumablesCategoryRepository.save(consumablesCategory);
		else {
			throw new Exception(consumablesCategory.getCategory() + " is already available as ConsumablesCategory");
		}
	}

	public synchronized ConsumablesCategory deleteConsumablesCategoryById(String id) throws Exception {
		ConsumablesCategory consumablesCategory = consumablesCategoryRepository.findById(id).orElse(null);

		if (consumablesCategory != null) {
			// consumablesInventoryRepository.updateCategory(consumablesCategory.getCategory(), "");
			if (consumablesCategory.getConsumablesCount() != null && consumablesCategory.getConsumablesCount() > 0)
				throw new Exception("Cannot delete category as its has " + consumablesCategory.getConsumablesCount()
						+ " Consumables reffering to ");
		} else {
			throw new Exception("Invalid id for deleteConsumablesCategoryById " + id);
		}

		consumablesCategoryRepository.deleteById(id);
		return consumablesCategory;
	}

	public ConsumablesCategory updateConsumablesCategory(String oldCategory, String newCategory) {
		ConsumablesCategory consumablesCategory = consumablesCategoryRepository.findByCategory(oldCategory);
		if (consumablesCategory != null) {
			consumablesCategory.setCategory(newCategory);
			consumablesCategory = consumablesCategoryRepository.save(consumablesCategory);
			consumablesInventoryRepository.updateCategory(oldCategory, newCategory);
		}
		return consumablesCategory;
	}
	
}
