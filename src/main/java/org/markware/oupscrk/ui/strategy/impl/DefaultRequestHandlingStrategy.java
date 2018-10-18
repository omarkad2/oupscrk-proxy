package org.markware.oupscrk.ui.strategy.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.ui.protocol.payloads.RequestTamperingPayload;
import org.markware.oupscrk.ui.strategy.RequestHandlingStrategy;

/**
 * Default Request Handling Strategy
 * @author citestra
 *
 */
public class DefaultRequestHandlingStrategy implements RequestHandlingStrategy {

	/**
	 * Immutable headers
	 */
	private static final List<String> UNAUTHORIZED_TAMPER_HEADERS = Collections.unmodifiableList(
			Arrays.asList());
	
	/**
	 * Payload
	 */
	private RequestTamperingPayload payload;
	
	/**
	 * Constructor
	 * @param payload
	 */
	public DefaultRequestHandlingStrategy(String payload) {
		this.payload = RequestTamperingPayload.payloadDecoder(payload);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public HttpRequest updateRequest(HttpRequest httpRequest) {
		if (this.payload != null) {
			// Tamper with headers
			httpRequest.tamperWithHeaders(this.payload.getHeadersToTamper(), UNAUTHORIZED_TAMPER_HEADERS);
			// Tamper with body
			httpRequest.tamperWithBody(this.payload.getBodyReplacements());
		}
		return httpRequest;
	}

}
