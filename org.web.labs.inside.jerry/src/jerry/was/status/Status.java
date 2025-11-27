package org.web.labs.inside.jerry.was.status;

public enum Status {
	OK("200 OK"),
	CREATED("201 Created"),
	NO_CONTENT("204 No Content"),
	BAD_REQUEST("400 Bad Request"),
	UNAUTHORIZED("401 Unauthorized"),
	FORBIDDEN("403 Forbidden"),
	NOT_FOUND("404 Not Found"),
	METHOD_NOT_ALLOWED("405 Method Not Allowed"),
	INTERNAL_ERROR("500 Internal Server Error"),
	SERVICE_UNAVAILABLE("503 Service Unavailable");

	private final String text;

	private Status(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return this.text;
	}
}
