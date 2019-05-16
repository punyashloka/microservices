package com.microservices.netflix.zuul.api.gateway.server.service;

import com.microservices.netflix.zuul.api.gateway.server.bo.User;

public interface UserService {

	/*
	 * public String save(IntacctUser user);
	 * 
	 * public IntacctUser getLoggedInUser();
	 * 
	 * public String changePassword(Long userId, String password);
	 * 
	 * public String resetPassword(Long userId);
	 * 
	 * public String updateUser(User user) throws Exception;
	 * 
	 * public String updateStatus(Long email, String status) throws Exception;
	 * 
	 * public List<User> getUsersByRole(String role);
	 * 
	 * public User getUserById(Long userId);
	 */

	

	public User findByEmail(String userName);


}
