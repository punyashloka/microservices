package com.punyam.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.punyam.account.dto.Account;
import com.punyam.account.service.AccountService;

@RestController
@RequestMapping(value = "/account")
public class AccountControllere {
	@Autowired
	private AccountService service;

	@GetMapping("/findById/{id}")
	public ResponseEntity<Account> findAccountById(@PathVariable Long id) {
		HttpHeaders responseHeaders = new HttpHeaders();
		Account account = service.findById(id);
		return new ResponseEntity<Account>(account, responseHeaders, HttpStatus.OK);

	}

	@PostMapping("/createAccount")
	public ResponseEntity<Long> createAccount(@RequestBody Account account) {
		HttpHeaders responseHeaders = new HttpHeaders();
		Long id = service.createAccount(account);
		return new ResponseEntity<Long>(id, responseHeaders, HttpStatus.OK);
	}
	
	@GetMapping("/isUserExists/userName/{userName}")
	public ResponseEntity<Boolean> isUserExistsByUserName(@PathVariable String userName){
		HttpHeaders responseHeaders = new HttpHeaders();
		Boolean isUserExists = service.isUserExistsByUserName(userName);
		return new ResponseEntity<Boolean>(isUserExists, responseHeaders, HttpStatus.OK);
	}
	@GetMapping("/isUserExists/email/{email}")
	public ResponseEntity<Boolean> isUserExistsByEmail(@PathVariable String email){
		HttpHeaders responseHeaders = new HttpHeaders();
		Boolean isUserExists = service.isUserExistsByEmail(email);
		return new ResponseEntity<Boolean>(isUserExists, responseHeaders, HttpStatus.OK);
	}
	
}
