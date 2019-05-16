package com.microservices.netflix.zuul.api.gateway.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import com.google.common.collect.ImmutableList;

/**
 * 
 * @author 
 *
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Import(SecurityProblemSupport.class)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private CustomAuthenticationSuccessHandle customAuthenticationSuccessHandle;

	@Autowired
	private AuthenticationTokenProcessingFilter authenticationTokenProcessingFilter;

    private final SecurityProblemSupport problemSupport;

    @Autowired
    public SecurityConfig(SecurityProblemSupport problemSupport) {
    	this.problemSupport = problemSupport;
    }
    
	@Autowired
	@Lazy
	private UserService userService;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.exceptionHandling()
        .authenticationEntryPoint(problemSupport)
        .accessDeniedHandler(problemSupport);
		
		http.cors();
		http.authorizeRequests().antMatchers("/").permitAll()
				.antMatchers("/login", "/registration", "/forgotPwd", "/configuration/ui", "/webjars/**",
						"/swagger-ui.html", "/swagger-resources", "/configuration/security", "/v2/api-docs")
				.permitAll()
				.antMatchers("/intacctadpater/**").permitAll()
				.antMatchers("/**/api-docs").permitAll() // un-secure entry point
				.antMatchers(ExitEndpoint.EXIT_ENDPOINT).permitAll() // un-secure entry point				
				.antMatchers(DefaultRibbonConfiguration.PING_ENDPOINT).permitAll() // un-secure entry point
				.antMatchers(BuildDateEndpoint.BUILD_DATE_ENDPOINT).permitAll() // un-secure entry point
				.anyRequest().authenticated().and().csrf()
				.disable().formLogin().successHandler(customAuthenticationSuccessHandle)
				.failureHandler(new CustomAuthenticationFailureHandler()).and()
				.addFilterBefore(authenticationTokenProcessingFilter, UsernamePasswordAuthenticationFilter.class);
	}

	@Override
	public void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(getPasswordEncoder());
	}

	@Bean
	public PasswordEncoder getPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		final CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(ImmutableList.of("*"));
		configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
		// setAllowCredentials(true) is important, otherwise:
		// The value of the 'Access-Control-Allow-Origin' header in the response must
		// not be the wildcard '*' when the request's credentials mode is 'include'.
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		configuration.setExposedHeaders(ImmutableList.of("X-Auth-Token"));
		// setAllowedHeaders is important! Without it, OPTIONS preflight request
		// will fail with 403 Invalid CORS request
		configuration.setAllowedHeaders(ImmutableList.of("Content-Type", "x-requested-with", "X-Auth-Token",
				"authorization", "X-Auth-Remember-Me-Token", "Cache-Control"));
		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

}
