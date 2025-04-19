package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.ConsumablesCategory;

public interface ConsumablesCategoryRepository extends MongoRepository<ConsumablesCategory, String> {

	ConsumablesCategory findByCategory(String category);

}
