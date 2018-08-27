package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * Connection intercepter
 * @author citestra
 *
 */
public class ConnectionHandler implements Runnable {

	private static Object mutex = new Object();

	/**
	 * Client socket
	 */
	private Socket clientSocket;

	/**
	 * CA KEY
	 */
	private PrivateKey caKey;

	/**
	 * Intermediate Key
	 */
	private PrivateKey intKey;

	/**
	 * CA Cert
	 */
	private X509Certificate caCert;

	/**
	 * Intermediate Cert
	 */
	private X509Certificate intCert;

	/**
	 * CERT KEY
	 */
	private PrivateKey certKey;

	/**
	 * CERTS FOLDER
	 */
	private Path certsFolder;

	/**
	 * Read data client sends to proxy
	 */
	private InputStream proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	private DataOutputStream proxyToClientBw;

	/**
	 * Headers to remove
	 */
	private static final List<String> HEADERS_TO_REMOVE = Collections.unmodifiableList(
			Arrays.asList("connection", "keep-alive", "proxy-authenticate", 
					"proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade"));

	/**
	 * Buffer Size
	 */
	private static final int BUFFER_SIZE = 32768; // 65536

	/**
	 * Constructor
	 * @param clientSocket
	 */
	public ConnectionHandler(
			Socket clientSocket, 
			PrivateKey caKey, 
			PrivateKey intKey, 
			X509Certificate caCert, 
			X509Certificate intCert, 
			PrivateKey certKey, 
			Path certsFolder) {
		this.clientSocket = clientSocket;
		this.caKey = caKey;
		this.intKey = intKey;
		this.caCert = caCert;
		this.intCert = intCert;
		this.certKey = certKey;
		this.certsFolder = certsFolder;
		try{
			this.clientSocket.setSoTimeout(2000);
			this.proxyToClientBr = this.clientSocket.getInputStream();
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
		// 1 - Parse request from Client
		try{
			HttpRequestParser requestParsed = new HttpRequestParser();
			requestParsed.parseRequest(new BufferedReader(new InputStreamReader(this.proxyToClientBr)));

			if (requestParsed.getRequestType() != null && requestParsed.getUrl() != null) {

				// 2 - Forward request to Remote server
				switch(requestParsed.getRequestType()) {
				case "CONNECT":
					doConnect(requestParsed);
				default:
					doGet(requestParsed);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Shutting down " + e.getMessage());
		}
	}

	/**
	 * Get method handler
	 * @param requestParsed
	 */
	private void doGet(HttpRequestParser requestParsed) {
		System.out.println("Hostname : " + requestParsed.getHostname());
		try {
			URL url = requestParsed.getUrl();
			String protocolHttp = requestParsed.getScheme();

			if (url.toString().contains("oupscrk.local")) {
				sendCaCert(requestParsed);
			} else {
				// HTTP Connection
				HttpURLConnection conn;
				if ("https".equals(protocolHttp)) {
					conn = (HttpsURLConnection)url.openConnection();
				} else {
					conn = (HttpURLConnection)url.openConnection();
				}

				// Set Request Method
				conn.setRequestMethod("GET");

				// Set headers (filtering out proxy headers)
				if (requestParsed.getHeaders() != null) {
					requestParsed.getHeaders().entrySet().forEach((entry) -> {
						if (!HEADERS_TO_REMOVE.contains(entry.getKey())) {
							conn.setRequestProperty(entry.getKey(), entry.getValue());
						}
					});
				}

				// Send body if there is one
				String requestBody = requestParsed.getMessageBody();
				if (requestBody != null && !requestBody.isEmpty()) {
					conn.setRequestMethod("POST");
					conn.setDoOutput(true);
					OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);    
					osw.write(requestBody);
					osw.flush();
					osw.close(); 
				}

				conn.setReadTimeout(10000);
				conn.setConnectTimeout(10000);

				conn.setDoInput(true);
				conn.setAllowUserInteraction(false);
				conn.connect();

				// Get the response stream
				InputStream serverToProxyStream = null;
				int responseCode = conn.getResponseCode();
				if (responseCode >= 400) {
					serverToProxyStream = conn.getErrorStream();
				} else {
					serverToProxyStream = conn.getInputStream();
				}

				// Send response to client
				if (serverToProxyStream != null) {

					// Read body
					String contentEncoding = conn.getContentEncoding();
					ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
					byte by[] = new byte[ BUFFER_SIZE ];
					int index = serverToProxyStream.read(by, 0, BUFFER_SIZE);
					while ( index != -1 ) {
						responseBuffer.write(by, 0, index);
						index = serverToProxyStream.read( by, 0, BUFFER_SIZE );
					}
					responseBuffer.flush();

					// Decode body, Modify response ...
					byte[] responsePlain = CompressionUtils.decodeContentBody(responseBuffer.toByteArray(), contentEncoding);
//					String responsePlainStr = new String(responsePlain, StandardCharsets.UTF_8);
//					System.out.println(responsePlainStr);
					// encode response
					byte[] encodedResponse = CompressionUtils.encodeContentBody(responsePlain, contentEncoding);
					ByteArrayInputStream streamToSend = new ByteArrayInputStream(encodedResponse);

					// send statusLine
					String statusLine = conn.getHeaderField(0);
					this.proxyToClientBw.write(String.format("%s\r\n", statusLine).getBytes(StandardCharsets.UTF_8));

					// send headers (filtered)
					for(Entry<String, List<String>> header : conn.getHeaderFields().entrySet()) {
						if (header.getKey() != null && !HEADERS_TO_REMOVE.contains(header.getKey())) {
							this.proxyToClientBw.write(
									new StringBuilder().append(header.getKey())
									.append(": ")
									.append(String.join(", ", header.getValue()))
									.append("\r\n")
									.toString()
									.getBytes(StandardCharsets.UTF_8));
						}
					}

					// end headers
					this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));

					// Send encoded stream to client (navigator)
					byte[] bodyChunk = new byte [BUFFER_SIZE];
					int read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE);
					while ( read != -1 ) {
						this.proxyToClientBw.write(bodyChunk, 0, read);
						read = streamToSend.read(bodyChunk, 0, BUFFER_SIZE );
					}
					this.proxyToClientBw.flush();

					// Close Remote Server -> Proxy Stream
					if (serverToProxyStream != null) {
						serverToProxyStream.close();
					}
				}
			}
		} catch(IOException | DataFormatException | CertificateEncodingException e) {
			System.out.println("********* IO EXCEPTION **********: " + e);
		} finally {
			this.shutdown();
		}
	}
	public String readFullyAsString(InputStream inputStream, String encoding) throws IOException {
		return readFully(inputStream).toString(encoding);
	}

	private ByteArrayOutputStream readFully(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return baos;
	}

	private void sendCaCert(HttpRequestParser requestParsed) throws IOException, CertificateEncodingException {
		byte[] caCertData = this.caCert.getEncoded();
		this.proxyToClientBw.write(String.format("%s %d %s\r\n", requestParsed.getHttpVersion(), 200, "OK").getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Content-Type", "application/x-x509-ca-cert").getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Content-Length", caCertData.length).getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write(String.format("%s: %s\r\n", "Connection", "close").getBytes(StandardCharsets.UTF_8));

		this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));

		this.proxyToClientBw.write(caCertData);
		this.proxyToClientBw.flush();
	}

	/**
	 * Do connect handler
	 * @param requestParsed Client HTTP request
	 * @throws Exception 
	 */
	private void doConnect(HttpRequestParser requestParsed) throws Exception {
		if (this.caKey != null && this.caCert != null && this.certKey != null && this.certsFolder != null) {
			connectionIntercept(requestParsed);
		} else {
			System.out.println("CA resources missing -> aborting mission !");
			this.shutdown();
		}
	}

	/**
	 * Intercept HTTPS trafic (SSL handshake client <-> proxy) ------> In Progress
	 * @param requestParsed
	 * @throws Exception 
	 */
	private void connectionIntercept(HttpRequestParser requestParsed) throws Exception {
		String hostname = requestParsed.getUrl().getHost();
		String certFile = String.format("%s/%s.p12", this.certsFolder.toAbsolutePath().toString(), hostname);

		synchronized(mutex) {
			if (!new File(certFile).exists()) {
				SecurityUtils.createHostCert(hostname, certFile, this.certKey, this.caKey, this.intKey, this.caCert, this.intCert);
			}

			this.proxyToClientBw.write(String.format("%s %d %s\r\n", requestParsed.getHttpVersion(), 200, "Connection Established").getBytes(StandardCharsets.UTF_8));
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
							this.proxyToClientBr = this.clientSocket.getInputStream();
							this.proxyToClientBw = new DataOutputStream(this.clientSocket.getOutputStream());
							//							this.proxyToClientBw.write("<html>oussama</html>".getBytes());
							//							this.proxyToClientBw.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					});

			sslSocket.startHandshake();
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
