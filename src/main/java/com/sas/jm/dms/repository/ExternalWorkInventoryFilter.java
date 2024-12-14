package com.sas.jm.dms.repository;

import java.util.List;

import com.sas.jm.dms.entity.ExternalWorkInventory;

public interface ExternalWorkInventoryFilter {
	List<ExternalWorkInventory> findExternalWorkInventoryWithFilter(List<String> categoryList, String desc);
	void updateCategory(String oldCategory, String newCategory);
	
}
