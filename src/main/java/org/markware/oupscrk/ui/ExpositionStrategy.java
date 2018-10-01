package org.markware.oupscrk.ui;

import java.io.IOException;

import org.markware.oupscrk.objects.HttpRequest;
import org.markware.oupscrk.objects.HttpResponse;

public interface ExpositionStrategy {

	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException;
	
}
