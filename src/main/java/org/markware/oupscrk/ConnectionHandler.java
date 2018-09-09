package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.markware.oupscrk.utils.HttpRequest;
import org.markware.oupscrk.utils.HttpRequestParser;
import org.markware.oupscrk.utils.HttpResponse;
import org.markware.oupscrk.utils.HttpResponseParser;
import org.markware.oupscrk.utils.SecurityUtils;

/**
 * Connection intercepter
 * @author citestra
 *
 */
public class ConnectionHandler implements Runnable {

	/**
	 * Headers to remove
	 */
	private static final List<String> HEADERS_TO_REMOVE = Collections.unmodifiableList(
			Arrays.asList("Connection", "Proxy-Authenticate", "Keep-Alive", "Content-Length",
					"Proxy-Authorization", "te", "Trailers", "Transfer-Encoding", "Upgrade"));

	/**
	 * Buffer Size
	 */
	private static final int BUFFER_SIZE = 65536; // 32768 or 65536
	
	/**
	 * Lock
	 */
	private ReentrantLock fileLock = new ReentrantLock();

	/**
	 * Client socket
	 */
	private Socket clientSocket;

	/**
	 * SSL resources
	 */
	private SSLConfig sslResource;

	/**
	 * Read data client sends to proxy
	 */
	private BufferedReader proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	private DataOutputStream proxyToClientBw;

	/**
	 * Constructor
	 * @param clientSocket
	 */
	public ConnectionHandler(
			Socket clientSocket, 
			SSLConfig sslResource) {
		this.clientSocket = clientSocket;
		this.sslResource = sslResource;
		try{
			this.clientSocket.setSoTimeout(20000);
			this.proxyToClientBr = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			this.proxyToClientBw = new DataOutputStream(this.clientSocket.getOutputStream());
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Run method
	 */
	@Override
	public void run() {
		readClientRequest();
	}

	/**
	 * Intercept client's request
	 */
	public void readClientRequest() {
		try {
			HttpRequest requestParsed = HttpRequestParser.parseRequest(this.proxyToClientBr);
			if (requestParsed.getRequestType() != null && requestParsed.getUrl() != null) {
				switch(requestParsed.getRequestType()) {
				case "CONNECT":
					handleSSLHandshake(requestParsed);
					break;
				default:
					handleRequest(requestParsed);
					break;
				}
			}
		} catch (IOException e) {
			System.out.println("=> Error parsing request : " + e.getMessage());
			this.shutdown();
		}
	}
	
	/**
	 * Get method handler
	 * @param httpRequest
	 */
	private void handleRequest(HttpRequest httpRequest) {
		try {
			URL url = httpRequest.getUrl();
			String protocolHttp = httpRequest.getScheme();

			if (url.toString().contains("oupscrk.local")) {
				sendCaCert(httpRequest);
			} else {
				// HTTP Connection
				HttpURLConnection conn;
				if ("https".equals(protocolHttp)) {
					conn = (HttpsURLConnection)url.openConnection();
				} else {
					conn = (HttpURLConnection)url.openConnection();
				}

				// Set Request Method
				conn.setRequestMethod(httpRequest.getRequestType());

				// Set headers (filtering out proxy headers)
				if (httpRequest.getHeaders() != null) {
					filterHeaders(httpRequest.getHeaders()).forEach((key, value) -> {
						conn.setRequestProperty(key, value);
					});
				}

				// Send body if there is one
				String requestBody = httpRequest.getMessageBody();
				if (requestBody != null && !requestBody.isEmpty()) {
					conn.setDoOutput(true);
					OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);    
					osw.write(requestBody);
					osw.flush();
					osw.close(); 
				}

				conn.setReadTimeout(10000);
				conn.setConnectTimeout(10000);

				conn.connect();

				HttpResponse httpResponse = HttpResponseParser.parseResponse(conn);
				
				if (httpResponse.isNotBlank()) {
					// Send status line
					this.proxyToClientBw.write(String.format("%s\r\n", httpResponse.getStatusLine()).getBytes(StandardCharsets.UTF_8));
					
					// send headers (filtered)
					for (Entry<String, String> header : filterHeaders(httpResponse.getHeaders()).entrySet()) {
						this.proxyToClientBw.write(
								new StringBuilder().append(header.getKey())
												   .append(": ")
												   .append(header.getValue())
												   .append("\r\n")
												   .toString()
												   .getBytes(StandardCharsets.UTF_8));
					}
					
					this.proxyToClientBw.write(("Content-Length: " + httpResponse.getEncodedResponseBody().length+"\r\n").getBytes(StandardCharsets.UTF_8));
					
					// end headers
					this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));
					// Send encoded stream to client (navigator)
					ByteArrayInputStream streamToSend = new ByteArrayInputStream(httpResponse.getEncodedResponseBody());
					byte[] bodyChunk = new byte [BUFFER_SIZE];
					int read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE);
					while ( read != -1 ) {
						this.proxyToClientBw.write(bodyChunk, 0, read);
						read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE );
					}
					this.proxyToClientBw.flush();
					displayInfo(httpRequest, conn.getHeaderFields(), httpResponse.getPlainResponseBody());
					
				}
			}
		} catch(IOException | DataFormatException | CertificateEncodingException e) {
			System.out.println("*** DOGET EXCEPTION ***: " + httpRequest.getHostname() + " : " + e);
		} finally {
			this.shutdown();
		}
	}
	
	private Map<String, String> filterHeaders(Hashtable<String, String> headers) {
		Entry<String, String> allowedEncodings = 
				new AbstractMap.SimpleEntry<String, String>("Accept-Encoding", "gzip, deflate, identity, x-gzip");
		return headers.entrySet().stream().filter((header) -> !HEADERS_TO_REMOVE.contains(header.getKey()))
									.map((header) -> "Accept-Encoding".equals(header.getKey()) ? allowedEncodings : header)
								   .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	private void sendCaCert(HttpRequest httpRequest) throws IOException, CertificateEncodingException {
		byte[] caCertData = sslResource.getCaCert().getEncoded();
		this.proxyToClientBw.write(String.format("%s %d %s\r\n", httpRequest.getHttpVersion(), 200, "OK").getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Content-Type", "application/x-x509-ca-cert").getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Content-Length", caCertData.length).getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Connection", "close").getBytes(StandardCharsets.UTF_8));

		this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));

		this.proxyToClientBw.write(caCertData);
		this.proxyToClientBw.flush();
	}

	/**
	 * Do connect handler
	 * @param httpRequest Client HTTP request
	 * @throws Exception 
	 */
	private void handleSSLHandshake(HttpRequest httpRequest) {
		if (sslResource.isAllSet()) {
			completeSSLHandshake(httpRequest);
		} else {
			System.out.println("CA resources missing -> aborting mission !");
			this.shutdown();
		}
	}

	/**
	 * Intercept HTTPS trafic (SSL handshake client <-> proxy)
	 * @param httpRequest
	 * @throws Exception 
	 */
	private void completeSSLHandshake(HttpRequest httpRequest) {
		String hostname = httpRequest.getUrl().getHost();
		String certFile = String.format("%s/%s.p12", sslResource.getCertsFolder().toAbsolutePath().toString(), hostname);

		try {
			if (!new File(certFile).exists()) {
				fileLock.lock();
				try {
					SecurityUtils.createHostCert(hostname, certFile, sslResource);
				} finally {
					fileLock.unlock();
				}
			}

			this.proxyToClientBw.write(String.format("%s %d %s\r\n", httpRequest.getHttpVersion(), 200, "Connection Established").getBytes(StandardCharsets.UTF_8));
			this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));

			// wrap client socket
			FileInputStream is = new FileInputStream(certFile);
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(is, "secret".toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keyStore, "secret".toCharArray());

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), null, null);

			SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(
					this.clientSocket, 
					this.clientSocket.getInetAddress().getHostAddress(), 
					this.clientSocket.getPort(), 
					true);

			sslSocket.setUseClientMode(false);
			sslSocket.setNeedClientAuth(false);
			sslSocket.setWantClientAuth(false);
			sslSocket.addHandshakeCompletedListener(
					(HandshakeCompletedEvent handshakeCompletedEvent) -> {
						try {
							this.clientSocket = handshakeCompletedEvent.getSocket();
							this.proxyToClientBr = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
							this.proxyToClientBw = new DataOutputStream(this.clientSocket.getOutputStream());
							String connType = httpRequest.getHeaderParam("Proxy-Connection", "");
							if (! "close".equalsIgnoreCase(connType)) {
								readClientRequest();
							} else {
								this.shutdown();
							}
						} catch (IOException e) {
							System.out.println("Error in handshake callback " + httpRequest.getHostname() + " : " + e);
							this.shutdown();
						}
					});

			sslSocket.startHandshake();
		} catch (Exception e) {
			System.out.println("* DOCONNECT EXCEPTION *: " + httpRequest.getHostname() + " : " + e);
			this.shutdown();
		}
	}

	public synchronized void displayInfo(HttpRequest httpRequest, Map<String, List<String>> responseHeaders, String responseBody) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("logs/" + httpRequest.getHostname()));
		
		writer.write("The request : \n");
		writer.write(httpRequest.getRequestLine()+"\n");
		httpRequest.getHeaders().forEach((key, value) -> {
			try {
				writer.write(key + " : " + value+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		writer.write(httpRequest.getMessageBody()+"\n");
		
		writer.write("The response : \n");
		responseHeaders.forEach((key, value) -> {
			try {
				writer.write(key + " : " + String.join(", ", value)+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		writer.write(responseBody+"\n");
		
		writer.flush();
		writer.close();
	}
	
	/**
	 * Shutdown Connection
	 */
	private void shutdown() {
		try {
			if (this.proxyToClientBr != null) {
				this.proxyToClientBr.close();
			}
			if (this.proxyToClientBw != null) {
				this.proxyToClientBw.close();
			}
			if (this.clientSocket != null) {
				this.clientSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}