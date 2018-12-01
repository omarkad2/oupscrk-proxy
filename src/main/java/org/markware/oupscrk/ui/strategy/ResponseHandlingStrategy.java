package org.markware.oupscrk.ui.strategy;

import org.markware.oupscrk.http.HttpResponse;

public interface ResponseHandlingStrategy {

	/**
	 * Tamper with Reponse (if content type permits)
	 * @param httpResponse HTTP Response
	 * @param contentType Content type
	 * @return Tampered HTTP Response
	 */
	public HttpResponse updateResponse(HttpResponse httpResponse, String contentType);
}
