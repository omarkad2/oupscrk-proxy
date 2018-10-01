package org.markware.oupscrk.ui.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.markware.oupscrk.objects.HttpRequest;
import org.markware.oupscrk.objects.HttpResponse;
import org.markware.oupscrk.ui.ExpositionStrategy;

public class LogFileExpositionStrategy implements ExpositionStrategy {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("logs/" + httpRequest.getHostname()));
			
			writer.write("The request : \n");
			writer.write(httpRequest.getRequestLine()+"\n");
			for (Entry<String, String> entry : httpRequest.getHeaders().entrySet()) {
				writer.write(entry.getKey() + " : " + entry.getValue()+"\n");
			}
			writer.write(httpRequest.getMessageBody()+"\n");
			
			writer.write("The response : \n");
			for (Entry<String, String> entry : httpResponse.getHeaders().entrySet()) {
				writer.write(entry.getKey() + " : " + entry.getValue()+"\n");
			}
			writer.write(httpResponse.getPlainResponseBody()+"\n");
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

	
}
