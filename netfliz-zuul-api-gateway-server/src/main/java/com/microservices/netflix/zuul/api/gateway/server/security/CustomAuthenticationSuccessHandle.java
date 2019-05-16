package com.microservices.netflix.zuul.api.gateway.server.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.netflix.zuul.api.gateway.server.HeaderMapRequestWrapper;
import com.microservices.netflix.zuul.api.gateway.server.bo.AuthTokenTo;
import com.microservices.netflix.zuul.api.gateway.server.bo.Constants;
import com.microservices.netflix.zuul.api.gateway.server.bo.PrincipalUser;
import com.microservices.netflix.zuul.api.gateway.server.bo.ResponseTo;

@Component
public class CustomAuthenticationSuccessHandle implements AuthenticationSuccessHandler {

	@Value("${jwt.token.life.span}")
	private long tokenLifeSpan;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		HttpSession session = request.getSession();
		PrincipalUser authUser = (PrincipalUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		//session.setAttribute(Constants.USER_NAME, authUser.getUsername());
		//session.setAttribute(Constants.AUTHORITIES, authentication.getAuthorities());

		// set our response to OK status
		response.setStatus(HttpServletResponse.SC_OK);
		AuthTokenTo tokenTo = generateToken(authUser, request, tokenLifeSpan);
		tokenTo.setUserName(authUser.getUsername());
		HeaderMapRequestWrapper headerMapRequest = new HeaderMapRequestWrapper(request);
		headerMapRequest.setAttribute(Constants.USER_NAME, authUser.getUsername());
		headerMapRequest.addHeader(Constants.X_AUTH_TOKEN, tokenTo.getToken());
		tokenTo.setRole(authUser.getRoles().iterator().next().getAuthority());
		ResponseTo responseTo = new ResponseTo(0, Constants.SUCCESS, tokenTo);
		response.getWriter().append(objectMapper.writeValueAsString(responseTo));
	}
	
	public static AuthTokenTo generateToken(PrincipalUser userInfo, HttpServletRequest request, long tokenLifeSpan) {
		AuthTokenTo tokenTo = null;

		String userName = userInfo.getUsername();

		try {
			if (userName != null && !userName.isEmpty()) {
				tokenTo = new AuthTokenTo();
				Collection<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
				authorities.add(userInfo.getRoles().iterator().next());

				String password = "";
				Boolean enabled = true;
				Boolean accountNonExpired = true;
				Boolean credentialsNonExpired = true;
				Boolean accountNonLocked = true;

				PrincipalUser user = new PrincipalUser(userName, password, enabled, accountNonExpired,
						credentialsNonExpired, accountNonLocked, authorities, userInfo.getUserId());
				String encryptedToken = JwtTokenGenerator.createJsonWebToken(user, tokenLifeSpan,
						Constants.SIGNING_KEY, userName, user.getUsername(), null, null);
				tokenTo.setToken(encryptedToken);
				tokenTo.setUserId(userInfo.getUserId());
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
						user.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				return tokenTo;
			} else {
				throw new IllegalArgumentException(
						"Missing user name in login success, so unable to generate token...");
			}
		} catch (Exception e) {
		}
		return tokenTo;
	}
}
