package org.markware.oupscrk.proxy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import org.markware.oupscrk.config.SSLConfig;
import org.markware.oupscrk.http.HttpRequest;
import org.markware.oupscrk.http.HttpResponse;
import org.markware.oupscrk.http.parser.HttpRequestParser;
import org.markware.oupscrk.http.parser.HttpResponseParser;
import org.markware.oupscrk.ui.strategy.ExpositionStrategy;
import org.markware.oupscrk.ui.strategy.ReplayAttackStrategy;
import org.markware.oupscrk.ui.strategy.RequestHandlingStrategy;
import org.markware.oupscrk.ui.strategy.ResponseHandlingStrategy;
import org.markware.oupscrk.utils.SecurityUtils;

/**
 * Connection handler
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
	 * Read data client
	 */
	private BufferedReader proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	private DataOutputStream proxyToClientBw;

	/**
	 * Exposition Strategy
	 */
	private ExpositionStrategy expositionStrategy;
	
	/**
	 * Request handling strategy
	 */
	private RequestHandlingStrategy requestHandlingStrategy;
	
	/**
	 * Response handling strategy
	 */
	private ResponseHandlingStrategy responseHandlingStrategy;
	
	/**
	 * Replay attack strategy
	 */
	private ReplayAttackStrategy replayAttackStrategy;
	
	/**
	 * Default Constructor
	 */
	public ConnectionHandler() {}
	
	/**
	 * Set client socket
	 * @param sslResource
	 * @return connection handler
	 */
	public ConnectionHandler withClientSocket(Socket clientSocket) {
		try{
			this.clientSocket = clientSocket;
			this.clientSocket.setSoTimeout(20000);
			this.proxyToClientBr = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			this.proxyToClientBw = new DataOutputStream(this.clientSocket.getOutputStream());
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	/**
	 * Set sslConfig
	 * @param sslResource
	 * @return connection handler
	 */
	public ConnectionHandler withSSLConfig(SSLConfig sslResource) {
		this.sslResource = sslResource;
		return this;
	}
	
	/**
	 * Set exposition strategy
	 * @param expositionStrategy
	 * @return connection handler
	 */
	public ConnectionHandler withExpositionStrategy(
			ExpositionStrategy expositionStrategy) {
		this.expositionStrategy = expositionStrategy;
		return this;
	}

	/**
	 * Set request handling strategy
	 * @param requestHandlingStrategy
	 * @return connection handler
	 */
	public ConnectionHandler withRequestHandlingStrategy(
			RequestHandlingStrategy requestHandlingStrategy) {
		this.requestHandlingStrategy = requestHandlingStrategy;
		return this;
	}
	
	/**
	 * Set response handling strategy
	 * @param responseHandlingStrategy
	 * @return connection handler
	 */
	public ConnectionHandler withResponseHandlingStrategy(
			ResponseHandlingStrategy responseHandlingStrategy) {
		this.responseHandlingStrategy = responseHandlingStrategy;
		return this;
	}
	
	/**
	 * Set replay attack strategy
	 * @param replayAttackStrategy
	 * @return
	 */
	public ConnectionHandler withReplayAttackStrategy(
			ReplayAttackStrategy replayAttackStrategy) {
		this.replayAttackStrategy = replayAttackStrategy;
		return this;
	}
	
	/**
	 * Run method
	 */
	@Override
	public void run() {
		handleClientConnection();
	}

	/**
	 * Intercept client's request
	 */
	public void handleClientConnection() {
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
			String protocolHttp = httpRequest.getScheme();

			if (httpRequest.getUrl().toString().contains("oupscrk.local")) {
				sendCaCert(httpRequest);
			} else {
				// Engage replay attack ?
				engageReplayAttack(httpRequest);
				
				// Tamper with Request
				httpRequest = tamperHttpRequest(httpRequest);
				
				URL url = httpRequest.getUrl();
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
					OutputStream os = conn.getOutputStream();
					byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);
					DataOutputStream osw = new DataOutputStream(os);    
					osw.write(postData);
					osw.flush();
					osw.close(); 
					os.close();
				}

				conn.setReadTimeout(10000);
				conn.setConnectTimeout(10000);

				conn.connect();
				
				HttpResponse httpResponse = HttpResponseParser.parseResponse(conn);
				
				if (httpResponse.isNotBlank()) {
					// Tamper with Response
					httpResponse = tamperHttpResponse(httpResponse);
					
					// Send status line
					this.proxyToClientBw.write(String.format("%s\r\n", httpResponse.getStatusLine()).getBytes(StandardCharsets.UTF_8));
					
					// send headers (filtered)
					System.out.println(httpRequest.getRequestLine() + " : " + conn.getContentType());
					for (Entry<String, String> header : filterHeaders(httpResponse.getHeaders()).entrySet()) {
						this.proxyToClientBw.write(
								new StringBuilder().append(header.getKey())
												   .append(": ")
												   .append(header.getValue())
												   .append("\r\n")
												   .toString()
												   .getBytes(StandardCharsets.UTF_8));
					}
					
					this.proxyToClientBw.write(("Content-Length: " + httpResponse.retrieveEncodedResponseBody().length+"\r\n").getBytes(StandardCharsets.UTF_8));
					
					// end headers
					this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));
					// Send encoded stream to client (navigator)
					ByteArrayInputStream streamToSend = new ByteArrayInputStream(httpResponse.retrieveEncodedResponseBody());
					byte[] bodyChunk = new byte [BUFFER_SIZE];
					int read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE);
					while ( read != -1 ) {
						this.proxyToClientBw.write(bodyChunk, 0, read);
						read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE );
					}
					this.proxyToClientBw.flush();
					displayInfo(httpRequest, httpResponse);
					
				}
			}
		} catch(IOException | DataFormatException | CertificateEncodingException e) {
			System.out.println("*** DOGET EXCEPTION ***: " + httpRequest.getHostname() + " : " + e);
		} finally {
			this.shutdown();
		}
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
			System.out.println("SSL configuration missing -> abort mission !");
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
								handleClientConnection();
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

	/**
	 * Filter-out proxy specific headers
	 * @param headers
	 * @return filtered headers
	 */
	private Map<String, String> filterHeaders(Hashtable<String, String> headers) {
		Entry<String, String> allowedEncodings = 
				new AbstractMap.SimpleEntry<String, String>("Accept-Encoding", "identity, gzip, deflate, x-gzip");
		return headers.entrySet().stream().filter((header) -> !HEADERS_TO_REMOVE.contains(header.getKey()))
									.map((header) -> "Accept-Encoding".equals(header.getKey()) ? allowedEncodings : header)
								    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	/**
	 * Send CA cert to client (navigator)
	 * @param httpRequest
	 * @throws IOException
	 * @throws CertificateEncodingException
	 */
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
	 * Tamper with http request
	 * @param httpRequest
	 * @return tampered request
	 */
	private HttpRequest tamperHttpRequest(HttpRequest httpRequest) {
		if (this.requestHandlingStrategy != null) {
			this.requestHandlingStrategy.updateRequest(httpRequest);
		}
		return httpRequest;
	}

	/**
	 * Tamper with http response
	 * @param httpResponse
	 * @return tampered response
	 */
	private HttpResponse tamperHttpResponse(HttpResponse httpResponse) {
		if (this.responseHandlingStrategy != null) {
			this.responseHandlingStrategy.updateResponse(httpResponse);
		}
		return httpResponse;
	}
	
	/**
	 * Engage replay attack
	 * @param httpRequestCandidate
	 */
	private void engageReplayAttack(HttpRequest httpRequestCandidate) {
		if (this.replayAttackStrategy != null) {
			this.replayAttackStrategy.replay(httpRequestCandidate);
		}
	}
	
	/**
	 * Expose info
	 * @param httpRequest
	 * @param httpResponse
	 * @throws IOException
	 */
	public synchronized void displayInfo(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
		if (this.expositionStrategy != null) {
			this.expositionStrategy.exposeExchange(httpRequest, httpResponse);
		}
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
