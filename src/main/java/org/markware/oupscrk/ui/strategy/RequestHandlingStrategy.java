package org.markware.oupscrk.ui.strategy;

import org.markware.oupscrk.http.HttpRequest;

public interface RequestHandlingStrategy {

	public HttpRequest updateRequest(HttpRequest httpRequest);
}
