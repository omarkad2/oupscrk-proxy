package org.markware.oupscrk.ui.strategy;

import java.io.IOException;

import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;

/**
 * Exposition strategy
 * @author citestra
 *
 */
public interface ExpositionStrategy {

	/**
	 * Expose http exchange
	 * @param httpRequest
	 * @param httpResponse
	 * @param contentType
	 * @throws IOException
	 */
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse, String contentType) throws IOException;
	
}
