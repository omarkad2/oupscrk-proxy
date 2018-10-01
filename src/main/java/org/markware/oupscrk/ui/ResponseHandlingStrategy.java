package org.markware.oupscrk.ui;

import org.markware.oupscrk.objects.HttpResponse;

public interface ResponseHandlingStrategy {

	public HttpResponse updateResponse(HttpResponse httpResponse);
}
