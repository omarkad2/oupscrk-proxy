package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	private static final int BUFFER_SIZE = 4096;
	
	/**
	 * Constructor
	 * @param clientSocket
	 */
	public ConnectionHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
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
			System.out.println(requestParsed.getUrl());
			
			if (requestParsed.getRequestType() != null && requestParsed.getUrl() != null) {
				System.out.println(requestParsed.getRequestType() + " " + requestParsed.getUrl());
				// 2 - Forward request to client
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
			System.out.println("Shutting down" + e.getMessage());
		} finally {
			this.shutdown();
		}

	}

	
	private void doGet(HttpRequestParser requestParsed) {
		try {
			URL url = requestParsed.getUrl();
			URLConnection conn = url.openConnection();
			conn.setDoInput(true);
			//not doing HTTP posts
			conn.setDoOutput(false);

			// Get the response
			InputStream serverToProxyStream = null;
//			HttpURLConnection huc = (HttpURLConnection)conn;
			if (conn.getContentLength() > 0) {
				try {
					serverToProxyStream = conn.getInputStream();
				} catch (IOException ioe) {
					System.out.println("********* IO EXCEPTION **********: " + ioe);
				}
			}
			//end send request to server, get response from server
			///////////////////////////////////

			///////////////////////////////////
			//begin send response to client
			byte by[] = new byte[ BUFFER_SIZE ];
			int index = serverToProxyStream.read( by, 0, BUFFER_SIZE );
			while ( index != -1 )
			{
				this.proxyToClientBw.write( by, 0, index );
				index = serverToProxyStream.read( by, 0, BUFFER_SIZE );
			}
			this.proxyToClientBw.flush();

			if (serverToProxyStream != null) {
				serverToProxyStream.close();
			}
		} catch(IOException e) {
			System.out.println("********* IO EXCEPTION **********: " + e);
		} finally {
			this.shutdown();
		}
		
	}
	
	/**
	 * Do connect
	 * @param requestParsed Client HTTP request
	 */
	private void doConnect(HttpRequestParser requestParsed) {
		try {
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
			this.proxyToClientBw.write(line.getBytes("UTF-8"));
			this.proxyToClientBw.flush();

			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());

			clientToServerThread = new Thread(clientToServerHttps);
			clientToServerThread.start();


			// Listen to remote server and relay to client
			try {
				byte[] buffer = new byte[4096];
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
				this.proxyToClientBw.write(line.getBytes("UTF-8"));
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
				byte[] buffer = new byte[32768];
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
				// TODO: handle exception
			}
			catch (IOException e) {
				System.out.println("Proxy to client HTTPS read timed out");
				e.printStackTrace();
			}
		}
	}
	
//	private void doGet1(HttpRequestParser requestParsed) throws IOException, DataFormatException {
//		URL url = requestParsed.getUrl();
//		String httpMethod = requestParsed.getRequestType();
//		int contentLength = requestParsed.getHeaderParam("Content-Length") != null ? 
//				Integer.parseInt(requestParsed.getHeaderParam("Content-Length").trim()) : 0;
//		String requestBody = contentLength > 0 ? requestParsed.getMessageBody() : null;
//		
//		HttpClient httpClient = new HttpClient();
//		httpClient.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
//		GetMethod httpGet = new GetMethod(url.toString());
//		requestParsed.getHeaders().entrySet().forEach((entry)-> {
//			if (!HEADERS_TO_REMOVE.contains(entry.getKey())) {
//				httpGet.setRequestHeader(entry.getKey(), entry.getValue());
//			}
//		});
//		
//		httpClient.executeMethod(httpGet);
//		InputStream responseStream = httpGet.getResponseBodyAsStream();
//		
//		String encodingAlg = httpGet.getResponseHeader("Content-Encoding") != null ? 
//				httpGet.getResponseHeader("Content-Encoding").getValue() : null;
//
//		// Decode Response Body
////		String responsePlainBody = decodeContentBody(httpGet.getResponseBody(), encodingAlg);
//		
////		System.out.println(httpGet.getResponseBody().length);
//		// Store plain response body
////		System.out.println(responsePlainBody);
//		
//		// Encode Response Body
////		String responseBody = encodeContentBody(responsePlainBody, encodingAlg);
//		
//		// Send Response to client
//		this.proxyToClientBw.write(String.format("%s %d %s\r\n", httpGet.getStatusLine().getHttpVersion(), httpGet.getStatusLine().getStatusCode(), httpGet.getStatusLine().getReasonPhrase()).getBytes("UTF-8"));
//		
//		for (Header h : httpGet.getResponseHeaders()) {
//			if (! HEADERS_TO_REMOVE.contains(h.getName())) {
//				this.proxyToClientBw.write(new StringBuilder().append(h.getName()).append(": ").append(h.getValue()).append("\r\n").toString().getBytes("UTF-8"));
//			}
//		}
//		
//		this.proxyToClientBw.write("\r\n".getBytes("UTF-8"));
////		this.proxyToClientBw.write(httpGet.getResponseBody(), 0, httpGet.getResponseBody().length);
//		byte[] buffer = new byte[4096];
//		int read = 0;
//		try {
//			do {
//				read = responseStream.read(buffer);
//				if (read > 0) {
//					System.out.println(read);
//					this.proxyToClientBw.write(buffer, 0, read);
//	//				if (responseStream.available() < 1) {
//	//					this.proxyToClientBw.flush();
//	//				}
//				}
//			} while (responseStream != null && read >= 0);
//		} catch (SocketTimeoutException e) {
//			System.out.println("Socket timeout exception " + e.getMessage());
//		} catch (IOException e) {
//			System.out.println("IO exception " + e.getMessage());
//		}
//		responseStream.close();
//		this.proxyToClientBw.flush();
//		this.proxyToClientBw.close();
//		this.clientSocket.close();
//		
//		// TEST (works)
//		/*this.proxyToClientBw.write("HTTP/1.1 404 Not Found \r\n".getBytes("UTF-8"));
//		this.proxyToClientBw.write("Content-Type: text/HTML\r\n".getBytes("UTF-8"));
//		this.proxyToClientBw.write("Content-Length: 90\r\n".getBytes("UTF-8"));
//		this.proxyToClientBw.write("\r\n".getBytes("UTF-8"));
//		this.proxyToClientBw.write("<html><head><title>Page not found</title></head><body>The page was not found.</body></html>".getBytes("UTF-8"));
//		this.proxyToClientBw.flush();
//		this.proxyToClientBw.close();
//		this.clientSocket.close();*/
//	}
}
