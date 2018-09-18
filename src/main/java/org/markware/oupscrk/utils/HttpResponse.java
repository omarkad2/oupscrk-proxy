package org.markware.oupscrk.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Http Response
 * @author citestra
 *
 */
public class HttpResponse {

	/**
	 * Status line
	 */
	private String statusLine;
	
	/**
	 * response code
	 */
	private int responseCode;
	
	/**
	 * Headers
	 */
	private Hashtable<String, String> headers;
	
	/**
	 * Raw response body
	 */
	private byte[] rawResponseBody;
	
	/**
	 * Decoded response body
	 */
	private String plainResponseBody;
	
	/**
	 * Encoded response body
	 */
	private byte[] encodedResponseBody;

	/**
	 * Encoding algorithm
	 */
	private String contentEncoding;
	
	public boolean isNotBlank() {
		return this.statusLine != null && !this.statusLine.isEmpty() && this.headers!= null && !this.headers.isEmpty();
	}
	
	// ******************************* GETTERS / SETTERS ******************************************
	public String getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(String statusLine) {
		this.statusLine = statusLine;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public Hashtable<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Hashtable<String, String> headers) {
		this.headers = headers;
	}

	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = new Hashtable<String, String>();
		if (headers != null) {
			headers.forEach((key, value) -> {
				if (key != null) {
					this.headers.put(key, String.join(", ", value));
				}
			});
		}
	}
	
	public byte[] getRawResponseBody() {
		return rawResponseBody;
	}

	public void setRawResponseBody(byte[] rawResponseBody) throws IOException {
		this.rawResponseBody = rawResponseBody;
		this.plainResponseBody =  new String(rawResponseBody, StandardCharsets.UTF_8);
		this.encodedResponseBody = CompressionUtils.encodeContentBody(rawResponseBody, this.contentEncoding);
	}
	
	public String getPlainResponseBody() {
		return plainResponseBody;
	}

	public void setPlainResponseBody(String plainResponseBody) {
		this.plainResponseBody = plainResponseBody;
	}

	public byte[] getEncodedResponseBody() {
		return encodedResponseBody;
	}

	public void setEncodedResponseBody(byte[] encodedResponseBody) {
		this.encodedResponseBody = encodedResponseBody;
	}
	
	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}
}
