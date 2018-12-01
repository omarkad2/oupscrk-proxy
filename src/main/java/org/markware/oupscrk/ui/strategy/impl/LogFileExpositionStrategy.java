package org.markware.oupscrk.ui.strategy.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map.Entry;

import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.ui.strategy.ExpositionStrategy;

public class LogFileExpositionStrategy implements ExpositionStrategy {

	/**
	 * Log file format
	 */
	private static final String LOG_FILE_FORMAT = "logs/%s-%s.log";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exposeExchange(HttpRequest httpRequest, HttpResponse httpResponse, String contentType) throws IOException {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(
					String.format(LOG_FILE_FORMAT,  Instant.now().toEpochMilli(), httpRequest.getHostname())));
			
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
