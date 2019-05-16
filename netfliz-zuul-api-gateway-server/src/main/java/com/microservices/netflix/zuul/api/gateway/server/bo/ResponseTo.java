package com.microservices.netflix.zuul.api.gateway.server.bo;

public class ResponseTo {

	private Integer error;
	private String message;
	private Object responseObj;

	public ResponseTo() {
		super();
	}

	public ResponseTo(Integer error, String message, Object responseObj) {
		super();
		this.error = error;
		this.message = message;
		this.responseObj = responseObj;
	}

	public Integer getError() {
		return error;
	}

	public void setError(Integer error) {
		this.error = error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getResponseObj() {
		return responseObj;
	}

	public void setResponseObj(Object responseObj) {
		this.responseObj = responseObj;
	}

}
