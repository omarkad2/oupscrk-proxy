package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

public class HttpRequestParser {

    private String requestLine;
    private String requestType;
    private String hostname;
    private String path;
    private int port;
    
	private Hashtable<String, String> headers;
    private StringBuffer body;

    public HttpRequestParser() {
        headers = new Hashtable<String, String>();
        body = new StringBuffer();
    }

    /**
     * Parse and HTTP request.
     * 
     * @param request
     *            String holding http request.
     * @throws IOException
     *             If an I/O error occurs reading the input stream.
     * @throws HttpFormatException
     *             If HTTP Request is malformed
     */
    public void parseRequest(BufferedReader reader) throws IOException {

    	// REQUEST LINE
    	if (reader.ready()) {
    		setRequestLine(reader.readLine());
    	}
    	
    	// HEADER
    	if (reader.ready()) {
	        String header = reader.readLine();
	        while (header.length() > 0) {
	            appendHeaderParameter(header);
	            header = reader.readLine();
	        }
    	}
    	
    	// BODY
        if (reader.ready()) {
	        String bodyLine = reader.readLine();
	        while (bodyLine != null) {
	            appendMessageBody(bodyLine);
	            bodyLine = reader.readLine();
	        }
        }
    }

    /**
     * 
     * 5.1 Request-Line The Request-Line begins with a method token, followed by
     * the Request-URI and the protocol version, and ending with CRLF. The
     * elements are separated by SP characters. No CR or LF is allowed except in
     * the final CRLF sequence.
     * 
     * @return String with Request-Line
     */
    public String getRequestLine() {
        return requestLine;
    }

    private void setRequestLine(String requestLine) {
    	try {
	        if (requestLine == null || requestLine.length() == 0) {
	            System.out.println("Invalid Request-Line: " + requestLine);
	        }
	        this.requestLine = requestLine;
	        // Get the Request type
	 		this.requestType = requestLine.substring(0,requestLine.indexOf(' '));
	
	 		// remove request type and space
	 		String urlString = requestLine.substring(requestLine.indexOf(' ')+1);
	
	 		// Remove everything past next space
	 		urlString = urlString.substring(0, urlString.indexOf(' '));
	 		
	 		if(!urlString.substring(0,6).equals("http://")){
				String temp = "http://";
				urlString = temp + urlString;
			}
	 		
	 		String pieces[] = urlString.substring(7).split(":");
			URL url = new URL(pieces[0].startsWith("https://") 
								|| pieces[0].startsWith("http://") ? pieces[0] : "http://" + pieces[0]);
			this.port = pieces.length>1 ? Integer.valueOf(pieces[1]) : 80;
			this.hostname = url.getHost();
			this.path = url.getPath();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    private void appendHeaderParameter(String header) {
        int idx = header.indexOf(":");
        if (idx == -1) {
        	System.out.println("Invalid Header Parameter: " + header);
        	return;
        }
        headers.put(header.substring(0, idx), header.substring(idx + 1, header.length()));
    }

    /**
     * The message-body (if any) of an HTTP message is used to carry the
     * entity-body associated with the request or response. The message-body
     * differs from the entity-body only when a transfer-coding has been
     * applied, as indicated by the Transfer-Encoding header field (section
     * 14.41).
     * @return String with message-body
     */
    public String getMessageBody() {
        return body.toString();
    }

    private void appendMessageBody(String bodyLine) {
        body.append(bodyLine).append("\r\n");
    }

    /**
     * For list of available headers refer to sections: 4.5, 5.3, 7.1 of RFC 2616
     * @param headerName Name of header
     * @return String with the value of the header or null if not found.
     */
    public String getHeaderParam(String headerName){
        return headers.get(headerName);
    }
    
    public String getRequestType() {
		return requestType;
	}

	public String getHostname() {
		return hostname;
	}

	public String getPath() {
		return path;
	}

	public int getPort() {
		return port;
	}
}