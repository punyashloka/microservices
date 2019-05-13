package com.punyam.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.punyam.account.dto.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
public Optional<Account> findById(Long id);

public Account findByUserName(String userName);

public Account findByEmail(String email);




}
