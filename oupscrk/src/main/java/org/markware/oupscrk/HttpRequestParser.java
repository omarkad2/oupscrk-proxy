package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class HttpRequestParser {

	private static final List<String> HEADERS_TO_REMOVE = Collections.unmodifiableList(
		    Arrays.asList("connection", "keep-alive", "proxy-authenticate", 
			"proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade"));
	
    private String requestLine;
    private String command;
    private URL url;
    private String httpVersion;
    private String scheme;
    private String hostname;
    private String path;

	private int port;
    
	private Hashtable<String, String> headers;
    private StringBuffer body;

    public HttpRequestParser() {
        headers = new Hashtable<String, String>();
        body = new StringBuffer();
    }

    public void parseRequest(BufferedReader reader) throws IOException {
    	
    	if (reader.ready()) {
    		// REQUEST LINE
        	setRequestLine(reader);

        	// HEADER
        	setHeaders(reader);
        	
        	// BODY
//        	setBody(reader);
    	}
    }

    public void setHeaders(BufferedReader reader) throws IOException {
		String header = reader.readLine();
    	while (header.length() > 0) {
    		if ("\r\n".equals(header.trim())) {
    			// header ends
    			return;
    		} else {
    			appendHeaderParameter(header);
    			header = reader.readLine();
    		}
    	}
    }
    
    public void setBody(BufferedReader reader) throws IOException {
		String bodyLine = reader.readLine();
    	while (bodyLine != null) {
    		appendMessageBody(bodyLine);
    		if (reader.ready())
    			bodyLine = reader.readLine();
    	}
    }

    public String getRequestLine() {
        return requestLine;
    }

    private void setRequestLine(BufferedReader reader) throws IOException {
    	try {
    		String requestLine = reader.readLine();
	        if (requestLine == null || requestLine.length() == 0) {
	            System.out.println("Invalid Request-Line: " + requestLine);
	        }
	        this.requestLine = requestLine;
	        
	        // requestLine parts
	        String[] requestLineParts = requestLine.split(" ");
	        
	        // Get the Request type
	 		this.command = requestLineParts[0];
	
	 		String urlString = requestLineParts[1];
	 		
	 		this.httpVersion = requestLineParts[2];
	 		
	 		String[] pieces;
	 		if (urlString.startsWith("https://")) {
	 			this.scheme = "https";
	 			pieces = urlString.substring(8).split(":");
	 		} else if (urlString.startsWith("http://")) {
	 			this.scheme = "http";
	 			pieces = urlString.substring(7).split(":");
	 		} else {
	 			pieces = urlString.split(":");
	 		}
	 		this.port = pieces.length>1 ? Integer.valueOf(pieces[1]) : 80;
	 		
	 		if (this.scheme == null) {
	 			this.scheme = this.port == 443 ? "https" : "http";
	 		}
	 		
			this.url = new URL(pieces[0].startsWith("https://") 
								|| pieces[0].startsWith("http://") ? pieces[0] : this.scheme + "://" + pieces[0]);
			
			this.hostname = this.url.getHost();
			this.path = this.url.getPath();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    public Hashtable<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Hashtable<String, String> headers) {
		this.headers = headers;
	}

	private void appendHeaderParameter(String header) {
        int idx = header.indexOf(":");
        if (idx == -1) {
        	System.out.println("Invalid Header Parameter: " + header);
        	return;
        }
        String headerName = header.substring(0, idx);
        String headerValue = header.substring(idx + 1, header.length());
        if (!HEADERS_TO_REMOVE.contains(headerName)) {
        	headers.put(headerName, headerValue);
        }
    }

    public String getMessageBody() {
        return body.toString();
    }

    private void appendMessageBody(String bodyLine) {
        body.append(bodyLine).append("\r\n");
    }

    public String getHeaderParam(String headerName){
        return headers.get(headerName);
    }
    
    public String getRequestType() {
		return command;
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
	
	public URL getUrl() {
		return url;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public String getScheme() {
		return scheme;
	}
}