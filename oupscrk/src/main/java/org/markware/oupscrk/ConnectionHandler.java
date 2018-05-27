package org.markware.oupscrk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
	BufferedReader proxyToClientBr;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter proxyToClientBw;
	

	/**
	 * Thread that is used to transmit data read from client to server 
	 */
	private Thread clientToServerThread;
	
	/**
	 * Constructor
	 * @param clientSocket
	 */
	public ConnectionHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(2000);
			this.proxyToClientBr = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			this.proxyToClientBw = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
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
		// Get Request from client
		HttpRequestParser requestParsed = new HttpRequestParser();
		try{
			requestParsed.parseRequest(proxyToClientBr);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error parsing HTTP request from client" + e.getMessage());
			return;
		}

		String requestLine = requestParsed.getRequestLine();
		System.out.println(requestLine);
		
		try {
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party
			
			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(requestParsed.getHostname());
			
			System.out.println("HOSTNAME : " + requestParsed.getHostname());
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, requestParsed.getPort());
			proxyToServerSocket.setSoTimeout(5000);
						
 			if("CONNECT".equals(requestParsed.getRequestType())){
				// Send Connection established to the client
				String line = "HTTP/1.0 200 Connection established\r\n" +
						"Proxy-Agent: ProxyServer/1.0\r\n" +
						"\r\n";
				proxyToClientBw.write(line);
				proxyToClientBw.flush();
			} else {
				doGet(requestParsed);
			}
			
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
						clientSocket.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							clientSocket.getOutputStream().flush();
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
	
			if(proxyToClientBw != null){
				proxyToClientBw.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			try{
				proxyToClientBw.write(line);
				proxyToClientBw.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} 
		catch (Exception e){
			System.out.println("Error on HTTPS : " + requestParsed.getHostname());
			e.printStackTrace();
		}
	}

	private void doGet(HttpRequestParser requestParsed) {
		int contentLength = requestParsed.getHeaderParam("Content-Length") != null ? 
							Integer.parseInt(requestParsed.getHeaderParam("Content-Length")) : 0;
		String requestBody = contentLength > 0 ? requestParsed.getMessageBody() : null;
		
		if ('/' == requestParsed.getPath().charAt(0)) {
			
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
				byte[] buffer = new byte[4096];
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
}
