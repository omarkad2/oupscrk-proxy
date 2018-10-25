package org.markware.oupscrk.ui.strategy;

import org.markware.oupscrk.http.HttpResponse;

public interface ResponseHandlingStrategy {

	public HttpResponse updateResponse(HttpResponse httpResponse, String contentType);
}
