package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.ExternalWorkCategory;

public interface ExternalWorkCategoryRepository extends MongoRepository<ExternalWorkCategory, String> {

	ExternalWorkCategory findByCategory(String category);

}
