package com.microservices.netflix.zuul.api.gateway.server.security;

import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.RequestContextFilter;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.dtis.common.security.constants.SecurityConstants;
import com.dtis.common.security.jwt.IServiceTokenGenerator;
import com.microservices.netflix.zuul.api.gateway.server.HeaderMapRequestWrapper;
import com.microservices.netflix.zuul.api.gateway.server.bo.AuthTokenTo;
import com.microservices.netflix.zuul.api.gateway.server.bo.Constants;
import com.microservices.netflix.zuul.api.gateway.server.bo.PrincipalUser;
import com.microservices.netflix.zuul.api.gateway.server.service.UserService;

/**
 * 
 * @author 
 *
 */
@Component
public class AuthenticationTokenProcessingFilter extends RequestContextFilter {

	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationTokenProcessingFilter.class);

	@Value("${jwt.token.life.span}")
	private long tokenLifeSpan;

	@Value("${jwt.token.renewal.extra.time}")
	private long tokenRenewalExtraTime;

	@Value("${jwt.reset}")
	private boolean jwtReset;

	public static final String TOKEN = "token";

	@Autowired
	@Lazy
	private UserService userService;
	
	/*
	 * private final IServiceTokenGenerator serviceTokenGenerator;
	 * 
	 * @Autowired public AuthenticationTokenProcessingFilter(IServiceTokenGenerator
	 * serviceTokenGenerator) { super(serviceTokenGenerator);
	 * this.serviceTokenGenerator = serviceTokenGenerator; }
	 */

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		HttpServletRequest httpRequest = this.getAsHttpRequest(request);
		HttpServletResponse httpResponse = this.getAsHttpResponse(response);
		LOG.info("Request path--->" + httpRequest.getRequestURI());

		if (httpRequest.getRequestURI().contains("/registration") || httpRequest.getRequestURI().contains("/login")
				|| httpRequest.getRequestURI().contains("/forgotPwd")
				|| httpRequest.getRequestURI().contains("/intacctadapter")
				|| httpRequest.getRequestURI().contains("/swagger-resources")
				|| httpRequest.getRequestURI().contains("/webjars")
				|| httpRequest.getRequestURI().contains("/v2/api-docs")
				|| httpRequest.getRequestURI().contains("validatesession")
				|| httpRequest.getRequestURI().contains("/swagger-ui.html")) {

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(null, null,
					null);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			chain.doFilter(request, response);
			return;
		}

		try {
			String authTokenStr = this.extractAuthTokenFromRequest(httpRequest);

			if (authTokenStr != null) {
				processAndAutheniticateWithToken(authTokenStr, httpRequest);
				HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(httpRequest);
				requestWrapper.addHeader(SecurityConstants.DEFAULT_JWT_HEADER_KEY,
						serviceTokenGenerator.generateWithPrefix());
				chain.doFilter(requestWrapper, httpResponse);
			} else {
				super.doFilterInternal(httpRequest, httpResponse, chain);
				return;
			}

		} catch (TokenExpiredException e) {
			LOG.info("::: Token got experied..." + e);
			// Verify if token got expired in 30 mins or not, if yes then
			// regenerate new token
			String experiedToken = this.extractAuthTokenFromRequest(httpRequest);
			// Get token expiration age in msec
			Long expiredAge = JwtTokenGenerator.getJwtTokenAge(experiedToken);
			// Renewal jwt token when that got expired recently (less than
			// tokenRenewalExtraTime)
			if (JwtTokenGenerator.isCurrentBoot(experiedToken, jwtReset)
					&& (expiredAge != null && expiredAge <= tokenRenewalExtraTime)) {
				LOG.info("Regenerating a new token...");
				String newToken = JwtTokenGenerator.reGenerateJwtToken(experiedToken, tokenLifeSpan,
						Constants.SIGNING_KEY, userService);
				if (newToken != null) {
					httpResponse.setHeader(Constants.X_AUTH_TOKEN, newToken);
					HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(httpRequest);
					requestWrapper.addHeader(Constants.X_AUTH_TOKEN, newToken);
					requestWrapper.addHeader(SecurityConstants.DEFAULT_JWT_HEADER_KEY,
							serviceTokenGenerator.generateWithPrefix());
					chain.doFilter(requestWrapper, httpResponse);
					return;
				} else {
					LOG.debug("Unable to regenerating the new token... ");
					httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
				}
			} else {
				httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} catch (JarException e) {
			LOG.warn("::: Token got modified... " + e);
		} catch (SignatureException e) {
			LOG.warn("::: Token signature got modified... " + e);
			SecurityContextHolder.clearContext();
			httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
		} catch (Exception ex) {
			LOG.error("::: Exception occured : " + ex);
			SecurityContextHolder.clearContext();
			httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
		}
	}

	private HttpServletRequest getAsHttpRequest(ServletRequest request) {
		if (!(request instanceof HttpServletRequest)) {
			throw new RuntimeException("::: Expecting an HTTP request");
		}

		return (HttpServletRequest) request;
	}

	private HttpServletResponse getAsHttpResponse(ServletResponse response) {
		if (!(response instanceof HttpServletResponse)) {
			throw new RuntimeException("::: Expecting an HTTP request");
		}

		return (HttpServletResponse) response;
	}

	private String extractAuthTokenFromRequest(HttpServletRequest httpRequest) {
		/* Get token from header */
		String authToken = httpRequest.getHeader(Constants.X_AUTH_TOKEN);

		/* If token not found get it from request parameter */
		if (authToken == null) {
			authToken = httpRequest.getParameter(TOKEN);
		}
		return authToken;
	}

	public void init() {
		LOG.info("init");
	}

	@Override
	public void destroy() {
		LOG.info("destroy");
	}

	private final boolean processAndAutheniticateWithToken(String authToken, HttpServletRequest httpRequest)
			throws TokenExpiredException, SignatureException {

		String userName = null;
		// decode JWT token
		AuthTokenTo tokenTo = JwtTokenGenerator.verifyToken(authToken, Constants.SIGNING_KEY);
		LOG.info("Got decoded authToken: " + tokenTo.getUserName());
		if (tokenTo != null) {
			LOG.info("processAndAutheniticateWithToken...");
			// Getting user details object from jwt token
			UserDetails userDetails = getUserDetailsFromToken(tokenTo);
			if (userDetails != null) {
				userName = userDetails.getUsername();
			}
			if (userName != null && !"null".equalsIgnoreCase(userName)) {
				LOG.info("Authentication success with token...");
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));
				SecurityContextHolder.getContext().setAuthentication(authentication);
				return true;
			} else {
				LOG.info("User name not found in jwt token...");
			}
		}
		return false;
	}
	
	public static UserDetails getUserDetailsFromToken(AuthTokenTo authTokenTo) {
		if (authTokenTo == null) {
			return null;
		}
		Long userId = authTokenTo.getUserId();
		String userName = authTokenTo.getUserName();
		String password = authTokenTo.getPassword();
		List<SimpleGrantedAuthority> authoritiesList = new ArrayList<>();
		authoritiesList.add(new SimpleGrantedAuthority(authTokenTo.getRole()));
		return new PrincipalUser(userName, password, true, true, true, true, authoritiesList, userId);
	}
}
