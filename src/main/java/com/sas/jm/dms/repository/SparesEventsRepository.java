package com.sas.jm.dms.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.SparesEvents;

public interface SparesEventsRepository extends MongoRepository<SparesEvents, String> {
	List<SparesEvents> findAllByOrderByIdDesc();

}
