package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * 
 * @author citestra
 *
 */
public class ConnectionHandler implements Runnable {

	/**
	 * Client socket
	 */
	private Socket clientSocket;
	
	/**
	 * CA KEY
	 */
	private String caKey;
	
	/**
	 * CA CERT
	 */
	private String caCert;
	
	/**
	 * CERT KEY
	 */
	private String certKey;
	
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
	 * Thread that is used to transmit data read from client to server 
	 */
	private Thread clientToServerThread;

	/**
	 * Headers to remove
	 */
	private static final List<String> HEADERS_TO_REMOVE = Collections.unmodifiableList(
		    Arrays.asList("connection", "keep-alive", "proxy-authenticate", 
			"proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade"));
	
	/**
	 * Buffer Size
	 */
	private static final int BUFFER_SIZE = 32768;
	
	/**
	 * Constructor
	 * @param clientSocket
	 */
	public ConnectionHandler(Socket clientSocket, String caKey, String caCert, String certKey, Path certsFolder) {
		this.clientSocket = clientSocket;
		this.caKey = caKey;
		this.caCert = caCert;
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
	 * 
	 */
	@Override
	public void run() {
		// 1 - Parse request from Client
		try{
			HttpRequestParser requestParsed = new HttpRequestParser();
			requestParsed.parseRequest(new BufferedReader(new InputStreamReader(this.proxyToClientBr)));
			
			if (requestParsed.getRequestType() != null && requestParsed.getUrl() != null) {
//				System.out.println(requestParsed.getRequestType() + " " + requestParsed.getUrl());
				// 2 - Forward request to Remote server
				switch(requestParsed.getRequestType()) {
					case "CONNECT":
						doConnect(requestParsed);
					break;
					case "GET":
						doGet(requestParsed);
					break;
					default:
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Shutting down " + e.getMessage());
		} finally {
			this.shutdown();
		}
	}
	
	private void doGet(HttpRequestParser requestParsed) {
		try {
			URL url = requestParsed.getUrl();
			String protocolHttp = requestParsed.getScheme();
			
			// HTTP Connection
			HttpURLConnection conn;
			if ("https".equals(protocolHttp)) {
				conn = (HttpsURLConnection)url.openConnection();
			} else {
				conn = (HttpURLConnection)url.openConnection();
			}
			
			// Set Request Method
			conn.setRequestMethod(requestParsed.getRequestType());
			
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
				
				// send body
				String contentEncoding = conn.getContentEncoding();
				ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
				byte by[] = new byte[ BUFFER_SIZE ];
				int index = serverToProxyStream.read(by, 0, BUFFER_SIZE);
				while ( index != -1 ) {
					responseBuffer.write(by, 0, index);
					index = serverToProxyStream.read( by, 0, BUFFER_SIZE );
				}
				responseBuffer.flush();
				
				System.out.println(responseBuffer.size());
				// Decode body, Modify response ...
				byte[] responsePlain = CompressionUtils.decodeContentBody(responseBuffer.toByteArray(), contentEncoding);
//				String responsePlainStr = new String(responsePlain, StandardCharsets.UTF_8);
				
				// encode response and send it to client
				ByteArrayInputStream streamToSend = new ByteArrayInputStream(
						CompressionUtils.encodeContentBody(responsePlain, contentEncoding));
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
			
		} catch(IOException | DataFormatException e) {
			System.out.println("********* IO EXCEPTION **********: " + e);
		} finally {
			this.shutdown();
		}
	}
	
	/**
	 * Do connect
	 * @param requestParsed Client HTTP request
	 * @throws IOException 
	 */
	private void doConnect(HttpRequestParser requestParsed) throws IOException {
		if (this.caKey != null && !this.caKey.isEmpty() && this.caCert != null && !this.caCert.isEmpty() && this.certKey != null 
				&& !this.certKey.isEmpty() && this.certsFolder != null) {
			connectionIntercept(requestParsed);
		} else {
			connectionRelay(requestParsed);
		}
	}

	private void connectionIntercept(HttpRequestParser requestParsed) throws IOException {
		String hostname = requestParsed.getUrl().getHost();
		String certPath = String.format("%s/%s.crt", this.certsFolder.toAbsolutePath().toString(), hostname);
		
		// This chunk should be Thread safe !
		if (new File(certPath).exists()) {
			
		}
		
		this.proxyToClientBw.write(String.format("%s %d %s\r\n", requestParsed.getHttpVersion(), 200, "Connection Established").getBytes(StandardCharsets.UTF_8));
		this.proxyToClientBw.write("\r\n".getBytes(StandardCharsets.UTF_8));
		
		// wrap client socket
		SSLSocket sslSocket = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(
											this.clientSocket, 
											this.clientSocket.getInetAddress().getHostAddress(), 
											this.clientSocket.getPort(), 
							                true);
		sslSocket.setUseClientMode(false);
		sslSocket.startHandshake();
		this.clientSocket = sslSocket;
		this.proxyToClientBr = this.clientSocket.getInputStream();
		this.proxyToClientBw = new DataOutputStream(this.clientSocket.getOutputStream());
	}

	private void connectionRelay(HttpRequestParser requestParsed) {
		try {
			System.out.println("Relaying : " + requestParsed.getUrl());
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(requestParsed.getHostname());

			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, requestParsed.getPort());
			proxyToServerSocket.setSoTimeout(5000);
			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			this.proxyToClientBw.write(line.getBytes(StandardCharsets.UTF_8));
			this.proxyToClientBw.flush();

			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());

			clientToServerThread = new Thread(clientToServerHttps);
			clientToServerThread.start();


			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[BUFFER_SIZE];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						this.proxyToClientBw.write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							this.proxyToClientBw.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException e) {

			}
			catch (IOException e) {
				e.printStackTrace();
			}

			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				this.proxyToClientBw.write(line.getBytes(StandardCharsets.UTF_8));
				this.proxyToClientBw.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : ");
			e.printStackTrace();
		} finally {
			this.shutdown();
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
	
	/**
	 * Listen to data from client and transmits it to server.
	 * This is done on a separate thread as must be done 
	 * asynchronously to reading data from server and transmitting 
	 * that data to the client. 
	 */
	class ClientToServerHttpsTransmit implements Runnable{

		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;

		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[BUFFER_SIZE];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (SocketTimeoutException ste) {
				
			}
			catch (IOException e) {
				System.out.println("Proxy to client HTTPS read timed out");
				e.printStackTrace();
			}
		}
	}
	
}
