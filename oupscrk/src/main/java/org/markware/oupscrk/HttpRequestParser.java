package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

/**
 * Request Parser
 * @author citestra
 *
 */
public class HttpRequestParser {

	/**
	 * Request line
	 */
    private String requestLine;
    
    /**
     * Command
     */
    private String command;
    
    /**
     * URL
     */
    private URL url;
    
    /**
     * HttpVersion
     */
    private String httpVersion;
    
    /**
     * Scheme
     */
    private String scheme;
    
    /**
     * Hostname
     */
    private String hostname;
    
    /**
     * Path
     */
    private String path;

    /**
     * Port
     */
	private int port;
    
	/**
	 * Headers
	 */
	private Hashtable<String, String> headers;
	
	/**
	 * Body
	 */
    private StringBuffer body;

    /**
     * Constructor
     */
    public HttpRequestParser() {
        headers = new Hashtable<String, String>();
        body = new StringBuffer();
    }

    /**
     * Parse request
     * @param reader request reader
     * @throws IOException
     */
    public void parseRequest(BufferedReader reader) throws IOException {
    	
    	if (reader.ready()) {
    		// REQUEST LINE
        	setRequestLine(reader);

        	// HEADER
        	setHeaders(reader);
        	
        	// BODY
        	// setBody(reader);
    	}
    }

    /**
     * Set request line
     * @param reader
     * @throws IOException
     */
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
	 		if (this.command == null || this.command.isEmpty()) {
	 			throw new IOException("Command http Null");
	 		}
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
			
			if (this.url == null) {
				throw new IOException("Url Null");
			} else {
				this.hostname = this.url.getHost();
				this.path = this.url.getPath();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Set headers
     * @param reader
     * @throws IOException
     */
    public void setHeaders(BufferedReader reader) throws IOException {
		String header = reader.readLine();
    	while (header.length() > 0) {
			appendHeaderParameter(header);
			header = reader.readLine();
    	}
    }
    
    /**
     * Append header line to the rest of headers
     * @param header
     */
	private void appendHeaderParameter(String header) {
        int idx = header.indexOf(":");
        if (idx == -1) {
        	System.out.println("Invalid Header Parameter: " + header);
        	return;
        }
        String headerName = header.substring(0, idx);
        String headerValue = header.substring(idx + 1, header.length());
    	headers.put(headerName, headerValue);
    }
	
    /**
     * Set body
     * @param reader
     * @throws IOException
     */
    public void setBody(BufferedReader reader) throws IOException {
		String bodyLine = reader.readLine();
    	while (bodyLine.length() > 0) {
    		appendMessageBody(bodyLine);
    		if (reader.ready())
    			bodyLine = reader.readLine();
    	}
    }

    /**
     * Append body line to the rest of message body
     * @param bodyLine
     */
    private void appendMessageBody(String bodyLine) {
        body.append(bodyLine).append("\r\n");
    }
    
    // ******************************* GETTERS ******************************************
    public String getRequestLine() {
        return requestLine;
    }

    public String getMessageBody() {
        return body.toString();
    }

    public String getHeaderParam(String headerName){
        return headers.get(headerName);
    }
    
    public String getRequestType() {
		return command;
	}

    public void setRequestType(String command) {
		this.command = command;
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
	
	public Hashtable<String, String> getHeaders() {
		return headers;
	}

}