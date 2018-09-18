package org.markware.oupscrk.ui;

import org.markware.oupscrk.utils.HttpResponse;

public interface ResponseHandlingStrategy {

	public HttpResponse updateResponse(HttpResponse httpResponse);
}
