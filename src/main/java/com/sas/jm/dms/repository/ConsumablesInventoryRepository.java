package com.sas.jm.dms.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.ConsumablesInventory;

public interface ConsumablesInventoryRepository extends MongoRepository<ConsumablesInventory, String>, ConsumablesInventoryFilter{

	List<ConsumablesInventory> findAllByOrderByIdDesc();
	ConsumablesInventory findByDescAndCategory(String desc, String category);
	//void deleteByCategory(String category);
	Integer countByCategory(String category); 
}
