package com.sas.jm.dms.repository;

import java.util.List;

import com.sas.jm.dms.entity.LaborInventory;

public interface LaborInventoryFilter {
	List<LaborInventory> findLaborInventoryWithFilter(List<String> categoryList, String desc);
	void updateCategory(String oldCategory, String newCategory);
	
}
