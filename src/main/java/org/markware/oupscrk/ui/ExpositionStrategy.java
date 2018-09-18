package org.markware.oupscrk.ui;

import java.io.IOException;

import org.markware.oupscrk.utils.HttpRequest;
import org.markware.oupscrk.utils.HttpResponse;

public interface ExpositionStrategy {

	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException;
	
}
