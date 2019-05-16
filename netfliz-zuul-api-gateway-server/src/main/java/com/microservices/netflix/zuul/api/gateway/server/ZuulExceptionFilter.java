package com.microservices.netflix.zuul.api.gateway.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.exception.ZuulException;

@Component
public class ZuulExceptionFilter extends ZuulFilter{
private Logger logger = LoggerFactory.getLogger(ZuulExceptionFilter.class);
	@Override
	public boolean shouldFilter() {
		
		return true;
	}

	@Override
	public Object run() throws ZuulException {
		//Throwable throwable = RequestContext.getCurrentContext().getThrowable();
		//HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
		logger.info("request -> {} request uri- > {} exception ->{}",
				"");
		
		
		return null;
	}

	@Override
	public String filterType() {
		
		return "post";
	}

	@Override
	public int filterOrder() {
		
		 return Ordered.HIGHEST_PRECEDENCE;
	}

}
