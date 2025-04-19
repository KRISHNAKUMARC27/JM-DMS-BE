package com.sas.jm.dms.repository;

import java.util.List;

import com.sas.jm.dms.entity.ConsumablesInventory;

public interface ConsumablesInventoryFilter {
	List<ConsumablesInventory> findConsumablesInventoryWithFilter(List<String> categoryList, String desc);
	void updateCategory(String oldCategory, String newCategory);
	
}
