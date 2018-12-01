package org.markware.oupscrk.ui.strategy;

import org.markware.oupscrk.http.HttpRequest;

public interface RequestHandlingStrategy {

	/**
	 * Tamper with Request
	 * @param httpResponse HTTP Response
	 * @param contentType Content type
	 * @return Tampered HTTP Request
	 */
	public HttpRequest updateRequest(HttpRequest httpRequest);
}
