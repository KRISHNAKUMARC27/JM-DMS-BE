package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.SparesCategory;

public interface SparesCategoryRepository extends MongoRepository<SparesCategory, String> {

	SparesCategory findByCategory(String category);

}
