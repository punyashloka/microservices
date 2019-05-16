package com.microservices.netflix.zuul.api.gateway.server.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.microservices.netflix.zuul.api.gateway.server.bo.AuthTokenTo;
import com.microservices.netflix.zuul.api.gateway.server.bo.Constants;
import com.microservices.netflix.zuul.api.gateway.server.bo.PrincipalUser;
import com.microservices.netflix.zuul.api.gateway.server.bo.User;
import com.microservices.netflix.zuul.api.gateway.server.service.UserService;

import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.JsonTokenParser;
import net.oauth.jsontoken.crypto.HmacSHA256Signer;
import net.oauth.jsontoken.crypto.HmacSHA256Verifier;
import net.oauth.jsontoken.crypto.SignatureAlgorithm;
import net.oauth.jsontoken.crypto.Verifier;
import net.oauth.jsontoken.discovery.VerifierProvider;
import net.oauth.jsontoken.discovery.VerifierProviders;

@Component
public class JwtTokenGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenGenerator.class);

	public static String userId;

	@Value("${lifetime_auth_user}")
	public void setUserId(String userId) {
		JwtTokenGenerator.userId = userId;
	}

	private JwtTokenGenerator() {

	}

	/**
	 * Creates a json web token which is a digitally signed token that contains a
	 * payload (e.g. token to identify the user). The signing key is secret. That
	 * ensures that the token is authentic and has not been modified. Using a jwt
	 * eliminates the need to store authentication session information in a
	 * database.
	 * 
	 * @param firstName
	 * @param lastName
	 * @param tokenString
	 * @return
	 */
	public static String createJsonWebToken(UserDetails userDetails, Long tokenLifeSpan, String signKey,
			String userName, String email, String firstName, String lastName) {
		PrincipalUser intacctUser = (PrincipalUser) userDetails;
		// Current time and signing algorithm
		Calendar cal = Calendar.getInstance();
		HmacSHA256Signer signer;
		try {
			signer = new HmacSHA256Signer(Constants.ISSUER, null,
					signKey.getBytes(Charset.forName(Constants.UTF_8)));
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}

		// Configure JSON token
		JsonToken token = new JsonToken(signer);
		token.setIssuedAt(new Instant(cal.getTimeInMillis()));
		if (!userDetails.getUsername().equals(userId))
			token.setExpiration(new Instant(cal.getTimeInMillis() + tokenLifeSpan));
		// Roles(, seperated)
		StringBuilder roles = new StringBuilder();
		Collection<? extends GrantedAuthority> authorities = intacctUser.getAuthorities();
		if (authorities != null) {
			int authorityNumber = 1;
			for (GrantedAuthority grantedAuthority : authorities) {
				if (authorityNumber != 1) {
					roles.append(",");
				}
				roles.append(grantedAuthority.getAuthority());
				authorityNumber++;
			}
		}

		JsonObject request = new JsonObject();
		request.addProperty(Constants.ID, intacctUser.getUserId());
		request.addProperty(Constants.USER_NAME, intacctUser.getUsername());
		request.addProperty(Constants.PASSWORD, "");
		request.addProperty(Constants.ROLES, roles.toString());
		request.addProperty(Constants.EMAIL, email);
		request.addProperty(Constants.FIRST_NAME, firstName);
		request.addProperty(Constants.LAST_NAME, lastName);
		JsonObject payload = token.getPayloadAsJsonObject();
		payload.add(Constants.INFO, request);
		payload.remove(Constants.ISS);

		try {
			return token.serializeAndSign();
		} catch (SignatureException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Verifies a json web token's validity and extracts the token and other
	 * information from it.
	 * 
	 * @param token
	 * @return
	 * @throws JWTExpiredException
	 * @throws SignatureException
	 */
	public static AuthTokenTo verifyToken(String token, String signKey)
			throws TokenExpiredException, SignatureException {
		try {
			final Verifier hmacVerifier = new HmacSHA256Verifier(
					signKey.getBytes(StandardCharsets.UTF_8));

			VerifierProvider hmacLocator = new VerifierProvider() {

				public List<Verifier> findVerifier(String id, String key) {
					return Lists.newArrayList(hmacVerifier);
				}
			};
			VerifierProviders locators = new VerifierProviders();
			locators.setVerifierProvider(SignatureAlgorithm.HS256, hmacLocator);
			net.oauth.jsontoken.Checker checker = new net.oauth.jsontoken.Checker() {

				public void check(JsonObject payload) throws SignatureException {
					// don't throw - allow anything
				}

			};
			// Ignore Audience does not mean that the Signature is ignored
			JsonTokenParser parser = new JsonTokenParser(locators, checker);
			JsonToken jt;
			// get jwt-payload to verify whether it is expired or not
			JsonNode jwtPayload = decodeJwtPayload(token);
			if (jwtPayload != null) {
				verifyExpiration(jwtPayload);
			}
			jt = parser.verifyAndDeserialize(token);

			JsonObject payload = jt.getPayloadAsJsonObject();

			JsonObject info = payload.getAsJsonObject(Constants.INFO);
			String userId = info.getAsJsonPrimitive(Constants.ID).getAsString();
			String userName = info.getAsJsonPrimitive(Constants.USER_NAME).getAsString();
			String password = info.getAsJsonPrimitive(Constants.PASSWORD).getAsString();
			String role = info.getAsJsonPrimitive(Constants.ROLES).getAsString();
			AuthTokenTo tokenTo = new AuthTokenTo();
			tokenTo.setUserId(Long.valueOf(userId));
			tokenTo.setUserName(userName);
			tokenTo.setPassword(password);
			tokenTo.setRole(role);
			// account type
			return tokenTo;

		} catch (SignatureException e) {
			throw e;
		} catch (TokenExpiredException e) {
			throw e;
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utility to verify weather jwt token is experied or not
	 * 
	 * @param jwtClaims
	 * @throws JWTExpiredException
	 */
	private static void verifyExpiration(JsonNode jwtClaims) throws TokenExpiredException {
		// jwt token expiration time in sec so convert into msec
		final long expiration = jwtClaims.has(Constants.EXP)
				? jwtClaims.get(Constants.EXP).asLong(0) * 1000L
				: 0;
		if (expiration != 0 && System.currentTimeMillis() >= expiration) {
			throw new TokenExpiredException("jwt token got expired...");
		}
	}

	/**
	 * Takes a full jwt token and returns the payload decoded as a JsonNode
	 * 
	 * @param jwtString
	 * @return
	 */
	private static JsonNode decodeJwtPayload(String jwtString) {
		String[] pieces = jwtString.split("\\.");
		return decodeAndParse(pieces[1]);
	}

	/**
	 * Utility to convert payload to JsonNode, returns null if any json parsing
	 * exceptions because of token modifications
	 * 
	 * @param payload
	 * @return
	 */
	private static JsonNode decodeAndParse(String payload) {

		ObjectMapper mapper = new ObjectMapper();
		try {
			String jsonString = new String(Base64.decodeBase64(payload), "UTF-8");
			return mapper.readValue(jsonString, JsonNode.class);
		} catch (IOException e) {
			LOGGER.warn(
					"Error while parsing token ,it might be because of token has been modified... " + e.getMessage());
		}
		return null;
	}

	/**
	 * Utility to regenerate token for expired token
	 * 
	 * @param experiedToken
	 * @return
	 */
	public static String reGenerateJwtToken(String experiedToken, Long tokenLifeSpan, String signKey,
			UserService userService) {
		// Got JWT token expired so regenerate a new token
		// get payload from expired token
		JsonNode jwtPayload = decodeJwtPayload(experiedToken);
		if (jwtPayload != null && jwtPayload.get(Constants.INFO) != null) {
			// Generate new jwt token
			JsonNode jwtInfo = jwtPayload.get(Constants.INFO);
			String userId = jwtInfo.get(Constants.ID) != null ? jwtInfo.get(Constants.ID).asText() : null;
			String userName = jwtInfo.get(Constants.USER_NAME) != null
					? jwtInfo.get(Constants.USER_NAME).asText()
					: null;
			String password = jwtInfo.get(Constants.PASSWORD) != null
					? jwtInfo.get(Constants.PASSWORD).asText()
					: null;
			String roles = jwtInfo.get(Constants.ROLES) != null ? jwtInfo.get(Constants.ROLES).asText()
					: null;
			String email = jwtInfo.get(Constants.EMAIL) != null ? jwtInfo.get(Constants.EMAIL).asText()
					: null;
			String firstName = jwtInfo.get(Constants.FIRST_NAME) != null
					? jwtInfo.get(Constants.FIRST_NAME).asText()
					: null;
			String lastName = jwtInfo.get(Constants.LAST_NAME) != null
					? jwtInfo.get(Constants.LAST_NAME).asText()
					: null;

			List<SimpleGrantedAuthority> authoritiesList = null;
			if (roles != null && !roles.isEmpty()) {
				String[] rolesAry = roles.split(",");
				if (rolesAry != null && rolesAry.length > 0) {
					authoritiesList = new ArrayList<SimpleGrantedAuthority>();
					for (String role : rolesAry) {
						authoritiesList.add(new SimpleGrantedAuthority(role));
					}
				}
			}

			User intacctUser = userService.findByEmail(userName);
			PrincipalUser user = new PrincipalUser(userName, password, true, true, true, true, authoritiesList,
					Long.valueOf(userId));
			// Get account type
			if (intacctUser.getFirstName() != null) {
				return createJsonWebToken(user, tokenLifeSpan, signKey, intacctUser.getFirstName(), email, firstName,
						lastName);
			}
		}

		return null;
	}

	/**
	 * Utility to get time in msec from token expiration time point
	 * 
	 * @param expiredToken
	 * @return
	 */
	public static Long getJwtTokenAge(String expiredToken) {
		if (expiredToken == null || expiredToken.isEmpty()) {
			return null;
		}
		// get jwt-payload to verify weather it is expired or not
		JsonNode jwtPayload = decodeJwtPayload(expiredToken);
		if (jwtPayload != null) {
			// Jwt token expiration time in sec, so convert into msec
			final long expiration = jwtPayload.has(Constants.EXP)
					? jwtPayload.get(Constants.EXP).asLong(0) * 1000
					: 0;

			return System.currentTimeMillis() - expiration;
		}
		return null;
	}

	public static boolean isCurrentBoot(String expiredToken, boolean jwtReset) {
		if (expiredToken == null || expiredToken.isEmpty()) {
			return false;
		}
		JsonNode jwtPayload = decodeJwtPayload(expiredToken);
		if (jwtPayload != null) {
			final long start = jwtPayload.has(Constants.IAT) ? jwtPayload.get(Constants.IAT).asLong(0)
					: 0;
			boolean result = start - getStartTime() > 0;
			if (!jwtReset) {
				return true;
			}
			return result;
		}
		return false;
	}

	private volatile static Date date;

	public static Long getStartTime() {
		if (date == null) {
			synchronized (JwtTokenGenerator.class) {
				date = new Date();
			}
		}
		return date.getTime();
	}
}
