package org.markware.oupscrk.ui.strategy.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.ui.protocol.payloads.ResponseTamperingPayload;
import org.markware.oupscrk.ui.strategy.ResponseHandlingStrategy;

public class DefaultResponseHandlingStrategy implements ResponseHandlingStrategy {

	/**
	 * Immutable headers
	 */
	private static final List<String> UNAUTHORIZED_TAMPER_HEADERS = Collections.unmodifiableList(
			Arrays.asList("Content-Length"));
	
	/**
	 * Payload
	 */
	private ResponseTamperingPayload payload;
	
	/**
	 * Constructor
	 * @param payload
	 */
	public DefaultResponseHandlingStrategy(String payload) {
		this.payload = ResponseTamperingPayload.payloadDecoder(payload);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpResponse updateResponse(HttpResponse httpResponse, String contentType) {
		if (this.payload != null) {
			// Tamper with headers
			httpResponse.tamperWithHeaders(this.payload.getHeadersToTamper(), UNAUTHORIZED_TAMPER_HEADERS);
			// Tamper with body
			try {
				httpResponse.tamperWithBody(this.payload.getBodyReplacements(), contentType);
			} catch (IOException e) {
				System.out.println("Unable to tamper with response body");
			}
		}
		return httpResponse;
	}

}
