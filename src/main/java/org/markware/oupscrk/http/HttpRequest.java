package org.markware.oupscrk.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Request Parser
 * @author citestra
 *
 */
public class HttpRequest {

	/**
	 * Request line
	 */
    private String requestLine;
    
    /**
     * Command
     */
    private String command;
    
    /**
     * Scheme (HTTP or HTTPS)
     */
    private String scheme;
    
    /**
     * Path
     */
    private String path;
    
    /**
     * Query
     */
    private String query;
    
    /**
     * Hostname
     */
    private String hostname;
    
    /**
     * Port
     */
	private int port;
	
	/**
	 * RawUrl (/example/test.jpg, http://example.com/, example.com:443, http://example.com/test?id=1 ...) 
	 */
	private String rawUri;
	
	/**
     * URL
     */
    private URL url;
    
    /**
     * HttpVersion
     */
    private String httpVersion;
    
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
    public HttpRequest() {
        headers = new Hashtable<String, String>();
        body = new StringBuffer();
    }
    
    /**
     * Copy Constructor
     * @param copyFrom
     */
    public HttpRequest(HttpRequest copyFrom) {
    	this.requestLine = copyFrom.getRequestLine();
    	this.command = copyFrom.getRequestType();
    	this.scheme = copyFrom.getScheme();
    	this.path = copyFrom.getPath();
    	this.query = copyFrom.getQuery();
    	this.hostname = copyFrom.getHostname();
    	this.port = copyFrom.getPort();
    	this.rawUri = copyFrom.getRawUri();
    	this.url = copyFrom.getUrl();
    	this.httpVersion = copyFrom.getHttpVersion();
    	this.headers = copyFrom.getHeaders();
    	this.body = copyFrom.getBody();
    }

    /**
     * Append header line to the rest of headers
     * @param header
     */
	public void appendHeaderParameter(String header) {
        int idx = header.indexOf(":");
        if (idx == -1) {
        	System.out.println("Invalid Header Parameter: " + header);
        	return;
        }
        String headerName = header.substring(0, idx);
        String headerValue = header.substring(idx + 1, header.length()).replaceAll("\\s","");
    	headers.put(headerName, headerValue);
    }
	
	/**
     * Append body line to the rest of message body
     * @param bodyLine
     */
    public void appendMessageBody(String bodyLine) {
        body.append(bodyLine).append("\r\n");
    }
    
    /**
     * Build HttpRequest attributes
     * @throws IOException
     */
    public void interpretRawUri() throws IOException {
    	if (this.path.startsWith("/")) {
    		buildUrl();
		} else {
			String[] pieces;
			if (this.path.startsWith("https://")) {
				this.scheme = "https";
				pieces = this.path.substring(8).split(":");
			} else if (this.path.startsWith("http://")) {
				this.scheme = "http";
				pieces = this.path.substring(7).split(":");
			} else {
				pieces = this.path.split(":");
			}
			this.port = pieces.length>1 ? Integer.valueOf(pieces[1]) : 80;

			if (this.scheme == null) {
				this.scheme = this.port == 443 ? "https" : "http";
			}
			this.url = new URL(pieces[0].startsWith("https://") 
					|| pieces[0].startsWith("http://") ? pieces[0] : this.scheme + "://" + pieces[0]);
		}

		if (this.url == null) {
			throw new IOException("Url Null");
		} else {
			this.hostname = this.url.getHost();
		}
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
     */
    public void tamperWithBody(Map<String, String> replacements) {
    	if (replacements != null) {
    		replacements.entrySet().stream().forEach((entry) -> {
    			this.body = new StringBuffer(
    					this.body.toString().replaceAll(entry.getKey(), entry.getValue()));
    			this.query = this.query.replaceAll(entry.getKey(), entry.getValue());
    			try {
    				buildUrl();
				} catch (MalformedURLException e) {	}
    		});
    	}
    }
    
    /**
     * @param str
     * @return if body or query of the request contains a string (str)
     */
    public boolean containString(String str) {
    	boolean contains = false;
    	if (str != null && str.length() > 0 && (this.body != null || this.query != null)) {
    		return this.body.toString().contains(str) || this.query.contains(str);
    	}
    	return contains;
    }
    
    /**
     * Replace a string in body or query
     * @param str string to be replaced
     * @param replacement string to inject
     * @return Tampered HttpRequest
     */
    public HttpRequest replaceString(String str, String replacement) {
    	HttpRequest httpRequest = new HttpRequest(this);
    	if (str != null && replacement != null && (this.body != null || this.query != null)) {
    		httpRequest.setQuery(this.query.replaceAll(str, replacement));
    		httpRequest.setBody(new StringBuffer(this.body.toString().replaceAll(str, replacement)));
    	}
    	return httpRequest;
    }
    
	/**
     * Build url 
     * @throws MalformedURLException
     */
    private void buildUrl() throws MalformedURLException {
    	this.url = this.query != null && !this.query.isEmpty() ? 
				new URL(String.format("https://%s%s?%s", getHeaderParam("Host"), this.path, this.query)) :
				new URL(String.format("https://%s%s", getHeaderParam("Host"), this.path));
    }
    
    // ******************************* GETTERS/SETTERS ******************************************
    public String getRequestLine() {
        return requestLine;
    }

    public String getMessageBody() {
        return body.toString();
    }

    public String getHeaderParam(String headerName){
        return headers.get(headerName);
    }
    
    public String getHeaderParam(String headerName, String otherwise){
        return headers.get(headerName) != null ? headers.get(headerName) : otherwise;
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
	
	public String getRawUri() {
		return rawUri;
	}
	
	public String getHttpVersion() {
		return httpVersion;
	}

	public String getScheme() {
		return scheme;
	}
	
	public String getQuery() {
		return this.query;
	}
	
	public Hashtable<String, String> getHeaders() {
		return headers;
	}

	public StringBuffer getBody() {
		return body;
	}

	public void setBody(StringBuffer body) {
		this.body = body;
	}

	public void setRequestLine(String requestLine) {
		this.requestLine = requestLine;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setRawUri(String uri) {
		this.rawUri = uri;
		String[] uriSplitted = uri.split("\\?");
		this.path = uriSplitted[0];
		this.query = uriSplitted.length > 1 ? uriSplitted[1] : "";
	}
	
	public void setUrl(URL url) {
		this.url = url;
	}

	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}
}