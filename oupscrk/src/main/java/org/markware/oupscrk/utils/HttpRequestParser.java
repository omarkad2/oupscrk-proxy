package org.markware.oupscrk.utils;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Http Request Parser
 * @author citestra
 *
 */
public class HttpRequestParser {

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
		// setBody(httpRequest, reader);

		httpRequest.interpretRawUri();
		System.out.println(String.valueOf(httpRequest.getUrl()));
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
		httpRequest.setCommand(requestLineParts[0]);
		httpRequest.setRawUri(requestLineParts[1]);
		httpRequest.setHttpVersion(requestLineParts[2]);
	}

	/**
	 * Set headers
	 * @param reader
	 * @throws IOException
	 */
	public static void setHeaders(HttpRequest httpRequest, BufferedReader reader) throws IOException {
		String header = reader.readLine();
		while (header.length() > 0) {
			httpRequest.appendHeaderParameter(header);
			header = reader.readLine();
		}
	}

	/**
	 * Set body
	 * @param reader
	 * @throws IOException
	 */
	public void setBody(HttpRequest httpRequest, BufferedReader reader) throws IOException {
		String bodyLine = reader.readLine();
		while (bodyLine.length() > 0) {
			httpRequest.appendMessageBody(bodyLine);
			if (reader.ready())
				bodyLine = reader.readLine();
		}
	}

}
