package com.microservices.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.microservices.gateway.bo.ApplicationUser;

public interface UserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);

}
