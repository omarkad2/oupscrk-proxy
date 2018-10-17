package org.markware.oupscrk.ui.strategy;

import java.io.IOException;

import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;

public interface ExpositionStrategy {

	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException;
	
}
