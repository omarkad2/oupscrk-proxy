package org.markware.oupscrk.ui;

import org.markware.oupscrk.objects.HttpRequest;

public interface RequestHandlingStrategy {

	public HttpRequest updateRequest(HttpRequest httpRequest);
}
