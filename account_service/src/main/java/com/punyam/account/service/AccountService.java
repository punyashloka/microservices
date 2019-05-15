package com.punyam.account.service;

import java.util.List;

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
	public Long createAccount(Account account) {
		account = repo.save(account);
		return account.getId();
	}

	public Boolean isUserExistsByUserName(String userName) {
		Account account = repo.findByUserName(userName);
		if (account == null) {
			return false;
		}
		return true;
	}
	
	public Boolean isUserExistsByEmail(String email) {
		Account account = repo.findByEmail(email);
		if (account == null) {
			return false;
		}
		return true;
	}
	public List<Account> findAll() {
		return repo.findAll();
	}
}
