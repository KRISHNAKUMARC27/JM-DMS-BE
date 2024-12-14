package com.sas.jm.dms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sas.jm.dms.entity.ExternalWorkCategory;
import com.sas.jm.dms.entity.ExternalWorkInventory;
import com.sas.jm.dms.model.ExternalWorkFilter;
import com.sas.jm.dms.repository.ExternalWorkCategoryRepository;
import com.sas.jm.dms.repository.ExternalWorkInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalWorkService {

	private final ExternalWorkCategoryRepository externalWorkCategoryRepository;
	private final ExternalWorkInventoryRepository externalWorkInventoryRepository;
	
	public List<?> findAll() {
		return externalWorkInventoryRepository.findAllByOrderByIdDesc();
	}
	
	public ExternalWorkInventory findById(String id) throws Exception {
		return externalWorkInventoryRepository.findById(id).orElse(null);
	}
	
	public ExternalWorkInventory save(ExternalWorkInventory externalWork) {
		
		String oldExternalWorkCategory = null;
		if (externalWork.getId() != null) {
			// the externalWork is getting updated.
			ExternalWorkInventory oldExternalWork = externalWorkInventoryRepository.findById(externalWork.getId()).orElse(null);
			if (oldExternalWork != null && !oldExternalWork.getCategory().equals(externalWork.getCategory())) {
				oldExternalWorkCategory = oldExternalWork.getCategory();
			}
		}
		
		externalWork = externalWorkInventoryRepository.save(externalWork);
		
		Integer externalWorkCategoryCount = externalWorkInventoryRepository.countByCategory(externalWork.getCategory());
		ExternalWorkCategory externalWorkCategory = externalWorkCategoryRepository.findByCategory(externalWork.getCategory());
		externalWorkCategory.setExternalWorkCount(externalWorkCategoryCount);
		externalWorkCategoryRepository.save(externalWorkCategory);

		if (oldExternalWorkCategory != null) {
			Integer oldExternalWorkCategoryCount = externalWorkInventoryRepository.countByCategory(oldExternalWorkCategory);
			ExternalWorkCategory oldExternalWorkCat = externalWorkCategoryRepository.findByCategory(oldExternalWorkCategory);
			oldExternalWorkCat.setExternalWorkCount(oldExternalWorkCategoryCount);
			externalWorkCategoryRepository.save(oldExternalWorkCat);
		}
		return externalWorkInventoryRepository.save(externalWork);
	}
	
	public List<ExternalWorkInventory> findExternalWorkInventoryWithFilter(ExternalWorkFilter externalWorkFilter) {
		return externalWorkInventoryRepository.findExternalWorkInventoryWithFilter(externalWorkFilter.categoryList(),
				externalWorkFilter.desc());
	}

	public List<?> findAllExternalWorkCategory() {
		return externalWorkCategoryRepository.findAll();
	}

	public ExternalWorkCategory saveExternalWorkCategory(ExternalWorkCategory externalWorkCategory) throws Exception {
		ExternalWorkCategory category = externalWorkCategoryRepository.findByCategory(externalWorkCategory.getCategory());
		if (category == null)
			return externalWorkCategoryRepository.save(externalWorkCategory);
		else {
			throw new Exception(externalWorkCategory.getCategory() + " is already available as ExternalWorkCategory");
		}
	}

	public synchronized ExternalWorkCategory deleteExternalWorkCategoryById(String id) throws Exception {
		ExternalWorkCategory externalWorkCategory = externalWorkCategoryRepository.findById(id).orElse(null);

		if (externalWorkCategory != null) {
			// externalWorkInventoryRepository.updateCategory(externalWorkCategory.getCategory(), "");
			if (externalWorkCategory.getExternalWorkCount() != null && externalWorkCategory.getExternalWorkCount() > 0)
				throw new Exception("Cannot delete category as its has " + externalWorkCategory.getExternalWorkCount()
						+ " ExternalWork reffering to ");
		} else {
			throw new Exception("Invalid id for deleteExternalWorkCategoryById " + id);
		}

		externalWorkCategoryRepository.deleteById(id);
		return externalWorkCategory;
	}

	public ExternalWorkCategory updateExternalWorkCategory(String oldCategory, String newCategory) {
		ExternalWorkCategory externalWorkCategory = externalWorkCategoryRepository.findByCategory(oldCategory);
		if (externalWorkCategory != null) {
			externalWorkCategory.setCategory(newCategory);
			externalWorkCategory = externalWorkCategoryRepository.save(externalWorkCategory);
			externalWorkInventoryRepository.updateCategory(oldCategory, newCategory);
		}
		return externalWorkCategory;
	}
	
}
