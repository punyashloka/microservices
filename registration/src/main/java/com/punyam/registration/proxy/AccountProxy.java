package com.punyam.registration.proxy;

import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.punyam.registration.dto.Account;

@FeignClient(name = "netflix-zuul-api-gateway-server")
@RibbonClient(name = "registration_service")
public interface AccountProxy {
	
	@PostMapping("/account_service/account/createAccount")
	public Long createAccount(@RequestBody Account account);

	@GetMapping("/account_service/account/findById/{id}")
	public ResponseEntity<Account> findAccountById(@PathVariable("id") Long id);

	@GetMapping("/account_service/account/isUserExists/email/{email}")
	public ResponseEntity<Boolean> isUserExistsByEmail(@PathVariable("email") String email);

	@GetMapping("/account_service/account/isUserExists/userName/{userName}")
	public ResponseEntity<Boolean> isUserExistsByUserName(@PathVariable("userName") String userName);
}
