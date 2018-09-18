package org.markware.oupscrk.ui;

import org.markware.oupscrk.utils.HttpRequest;

public interface RequestHandlingStrategy {

	public HttpRequest updateRequest(HttpRequest httpRequest);
}
