package org.markware.oupscrk.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.markware.oupscrk.utils.CompressionUtils;

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
	
    /**
     * Tamper with headers
     * @param tamperedHeaders
     */
    public void tamperWithHeaders(Map<String, String> tamperedHeaders, List<String> immutableHeaders) {
    	if (tamperedHeaders != null) {
    		tamperedHeaders.entrySet().stream().forEach((entry) -> {
    			if (!immutableHeaders.contains(entry.getKey())) {
    				this.headers.put(entry.getKey(), entry.getValue());
    			}
    		});
    	}
    }
    
    /**
     * Tamper with body
     * @param replacements
     * @throws IOException 
     */
    public void tamperWithBody(Map<String, String> replacements, String contentType) throws IOException {
    	if (replacements != null && !contentType.contains("image") && !contentType.contains("video")) {
    		replacements.entrySet().stream().forEach((entry) -> {
    			this.plainResponseBody = 
    					this.plainResponseBody.replaceAll(entry.getKey(), entry.getValue());
    		});
    		this.encodedResponseBody = CompressionUtils.encodeContentBody(
    				this.plainResponseBody.getBytes(StandardCharsets.UTF_8), this.contentEncoding);
    	}
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
	
	public byte[] retreiveRawResponseBody() {
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

	public byte[] retrieveEncodedResponseBody() throws IOException {
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
