package com.sas.jm.dms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sas.jm.dms.entity.Role;

public interface RoleRepository extends MongoRepository<Role, String> {
    Role findByName(String name);
}

