package com.sas.jm.dms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.LaborCategory;
import com.sas.jm.dms.entity.LaborInventory;
import com.sas.jm.dms.entity.LaborInventory;
import com.sas.jm.dms.entity.LaborCategory;
import com.sas.jm.dms.entity.LaborInventory;
import com.sas.jm.dms.model.LaborFilter;
import com.sas.jm.dms.repository.LaborCategoryRepository;
import com.sas.jm.dms.repository.LaborInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaborService {

	private final LaborCategoryRepository laborCategoryRepository;
	private final LaborInventoryRepository laborInventoryRepository;
	
	public List<?> findAll() {
		return laborInventoryRepository.findAllByOrderByIdDesc();
	}
	
	public LaborInventory findById(String id) throws Exception {
		return laborInventoryRepository.findById(id).orElse(null);
	}
	
	public LaborInventory save(LaborInventory labor) {
		
		String oldLaborCategory = null;
		if (labor.getId() != null) {
			// the labor is getting updated.
			LaborInventory oldLabor = laborInventoryRepository.findById(labor.getId()).orElse(null);
			if (oldLabor != null && !oldLabor.getCategory().equals(labor.getCategory())) {
				oldLaborCategory = oldLabor.getCategory();
			}
		}
		
		labor = laborInventoryRepository.save(labor);
		
		Integer laborCategoryCount = laborInventoryRepository.countByCategory(labor.getCategory());
		LaborCategory laborCategory = laborCategoryRepository.findByCategory(labor.getCategory());
		laborCategory.setLaborCount(laborCategoryCount);
		laborCategoryRepository.save(laborCategory);

		if (oldLaborCategory != null) {
			Integer oldLaborCategoryCount = laborInventoryRepository.countByCategory(oldLaborCategory);
			LaborCategory oldLaborCat = laborCategoryRepository.findByCategory(oldLaborCategory);
			oldLaborCat.setLaborCount(oldLaborCategoryCount);
			laborCategoryRepository.save(oldLaborCat);
		}
		return laborInventoryRepository.save(labor);
	}
	
	public List<LaborInventory> findLaborInventoryWithFilter(LaborFilter laborFilter) {
		return laborInventoryRepository.findLaborInventoryWithFilter(laborFilter.categoryList(),
				laborFilter.desc());
	}

	public List<?> findAllLaborCategory() {
		return laborCategoryRepository.findAll();
	}

	public LaborCategory saveLaborCategory(LaborCategory laborCategory) throws Exception {
		LaborCategory category = laborCategoryRepository.findByCategory(laborCategory.getCategory());
		if (category == null)
			return laborCategoryRepository.save(laborCategory);
		else {
			throw new Exception(laborCategory.getCategory() + " is already available as LaborCategory");
		}
	}

	public synchronized LaborCategory deleteLaborCategoryById(String id) throws Exception {
		LaborCategory laborCategory = laborCategoryRepository.findById(id).orElse(null);

		if (laborCategory != null) {
			// laborInventoryRepository.updateCategory(laborCategory.getCategory(), "");
			if (laborCategory.getLaborCount() != null && laborCategory.getLaborCount() > 0)
				throw new Exception("Cannot delete category as its has " + laborCategory.getLaborCount()
						+ " Labor reffering to ");
		} else {
			throw new Exception("Invalid id for deleteLaborCategoryById " + id);
		}

		laborCategoryRepository.deleteById(id);
		return laborCategory;
	}

	public LaborCategory updateLaborCategory(String oldCategory, String newCategory) {
		LaborCategory laborCategory = laborCategoryRepository.findByCategory(oldCategory);
		if (laborCategory != null) {
			laborCategory.setCategory(newCategory);
			laborCategory = laborCategoryRepository.save(laborCategory);
			laborInventoryRepository.updateCategory(oldCategory, newCategory);
		}
		return laborCategory;
	}
	
}
