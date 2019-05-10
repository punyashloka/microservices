package com.punyam.account.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.punyam.account.dto.Account;
import com.punyam.account.repository.AccountRepository;

@Component
public class AccountService {

	@Autowired
	private AccountRepository repo;
	
	
	public Account findById(Long id) {
		 return  repo.findById(id).orElse(null);
		
	}
}
