package com.punyam.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.punyam.account.dto.Account;
import com.punyam.account.service.AccountService;

@RestController
public class AccountControllere {
@Autowired
private AccountService service;
@GetMapping("/findById/{id}")
	public ResponseEntity<Account> findAccountById(@PathVariable Long id){
		HttpHeaders responseHeaders = new HttpHeaders();
		Account account = service.findById(id);
		return new ResponseEntity<Account>(account, responseHeaders, HttpStatus.OK);
		
	}
}
