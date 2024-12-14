package com.sas.jm.dms.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.ExternalWorkInventory;

public interface ExternalWorkInventoryRepository extends MongoRepository<ExternalWorkInventory, String>, ExternalWorkInventoryFilter{

	List<ExternalWorkInventory> findAllByOrderByIdDesc();
	ExternalWorkInventory findByDescAndCategory(String desc, String category);
	//void deleteByCategory(String category);
	Integer countByCategory(String category); 
}
