package com.sas.jm.dms.repository;

import java.util.List;

import com.sas.jm.dms.entity.SparesInventory;

public interface SparesInventoryFilter {
	List<SparesInventory> findSparesInventoryWithFilter(List<String> categoryList, String desc);
	void updateCategory(String oldCategory, String newCategory);
	
}
