package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.User;

public interface UserRepository extends MongoRepository<User, String> {
    User findByUsername(String username);
}
