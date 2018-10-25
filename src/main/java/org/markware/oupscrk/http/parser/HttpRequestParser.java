package org.markware.oupscrk.http.parser;

import java.io.BufferedReader;
import java.io.IOException;

import org.markware.oupscrk.http.HttpRequest;

/**
 * Http Request Parser
 * @author citestra
 *
 */
public class HttpRequestParser {

	/**
	 * Buffer size
	 */
	private static final int BUFFER_SIZE = 1024;
	
	/**
	 * Parse request
	 * @param reader request reader
	 * @throws IOException
	 */
	public static HttpRequest parseRequest(BufferedReader reader) throws IOException {

		HttpRequest httpRequest = new HttpRequest();
		
		// REQUEST LINE
		setRequestLine(httpRequest, reader);

		// HEADER
		setHeaders(httpRequest, reader);

		// BODY
		setBody(httpRequest, reader);

		httpRequest.interpretRawUri();
		
		return httpRequest;
	}

	/**
	 * Set request line
	 * @param reader
	 * @throws IOException
	 */
	private static void setRequestLine(HttpRequest httpRequest, BufferedReader reader) throws IOException {
		String requestLine = reader.readLine();
		if (requestLine == null || requestLine.length() == 0 || requestLine.split(" ").length != 3) {
			throw new IOException("Invalid Request-Line: " + requestLine);
		}
		String[] requestLineParts = requestLine.split(" ");
		httpRequest.setRequestLine(requestLine);
		httpRequest.setCommand(requestLineParts[0]);
		httpRequest.setRawUri(requestLineParts[1]);
		httpRequest.setHttpVersion(requestLineParts[2]);
	}

	/**
	 * Set headers
	 * @param reader
	 * @throws IOException
	 */
	private static void setHeaders(HttpRequest httpRequest, BufferedReader reader) throws IOException {
		String header = reader.readLine();
		while (header != null && header.length() > 0) {
			httpRequest.appendHeaderParameter(header);
			header = reader.readLine();
		}
	}

	/**
	 * Set body
	 * @param reader
	 * @throws IOException
	 */
	private static void setBody(HttpRequest httpRequest, BufferedReader reader) throws IOException {
		char[] bodyChunk = new char[BUFFER_SIZE];
		int read;
		while (reader.ready() && (read = reader.read(bodyChunk, 0, BUFFER_SIZE)) != -1 ) {
			httpRequest.appendMessageBody(new String(bodyChunk, 0, read));
		}
		
	}

}
