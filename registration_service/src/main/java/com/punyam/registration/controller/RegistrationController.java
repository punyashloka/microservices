package com.punyam.registration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.punyam.registration.dto.Account;
import com.punyam.registration.proxy.AccountProxy;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

	@Autowired
	private AccountProxy accountProxy;
	@PostMapping("/createAccount")
	public ResponseEntity<?> createAccount(@RequestBody Account account) throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		Long id = null;
		// calling by rest template
		/*RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		ObjectMapper mapper = new ObjectMapper();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String jsonObject = mapper.writeValueAsString(account);
		HttpEntity<String> entity = new HttpEntity<String>(jsonObject, headers);
		ResponseEntity<String> responseEntity = restTemplate.exchange("http://localhost:8510/account/createAccount",
				HttpMethod.POST, entity, String.class);
		id = Long.parseLong(responseEntity.getBody());*/
		
		
		//zuul
		
		// id = accountProxy.createAccount(account);
		String userName = account.getUserName();
		String email = account.getEmail();
		ResponseEntity<Boolean> userExistsByEmail = accountProxy.isUserExistsByEmail(email);
		if (Boolean.valueOf(userExistsByEmail.getBody())) {
			return new ResponseEntity<String>("User Exists", responseHeaders, HttpStatus.BAD_REQUEST);
		} else {
			ResponseEntity<Boolean> userExistsByUserName = accountProxy.isUserExistsByUserName(userName);
			if (Boolean.valueOf(userExistsByUserName.getBody())) {
				return new ResponseEntity<String>("User Name Exists", responseHeaders, HttpStatus.BAD_REQUEST);
			}
			return new ResponseEntity<Long>(id, responseHeaders, HttpStatus.OK);
		}
	}
}
