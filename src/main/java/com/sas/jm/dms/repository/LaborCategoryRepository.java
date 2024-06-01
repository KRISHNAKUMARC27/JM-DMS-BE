package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.LaborCategory;

public interface LaborCategoryRepository extends MongoRepository<LaborCategory, String> {

	LaborCategory findByCategory(String category);

}
