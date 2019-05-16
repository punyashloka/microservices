package com.microservices.netflix.zuul.api.gateway.server.bo;

import java.util.Collection;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 *
 * @author 
 *
 */

public class PrincipalUser extends User {

	private static final long serialVersionUID = -3531439484732724601L;

	private final Long userId;
	private final Collection<SimpleGrantedAuthority> roles;

	public PrincipalUser(String username, String password, boolean enabled, boolean accountNonExpired,
			boolean credentialsNonExpired, boolean accountNonLocked, Collection<SimpleGrantedAuthority> authorities,
			Long userId) {
		super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
		this.userId = userId;
		this.roles = authorities;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Long getUserId() {
		return userId;
	}

	public Collection<SimpleGrantedAuthority> getRoles() {
		return roles;
	}

}
