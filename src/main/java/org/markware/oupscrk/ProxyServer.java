package org.markware.oupscrk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.markware.oupscrk.ui.ExpositionStrategy;

/**
 * Main thread
 * @author citestra
 *
 */
public class ProxyServer {

	private ServerSocket proxySocket;

	private int port;

	private boolean proxyOn;

	private SSLConfig sslResource;

	private ExpositionStrategy expositionStrategy;
	
	/**
	 * Constructor
	 * @param port
	 * @param sslResource
	 * @throws IOException
	 */
	public ProxyServer(int port, SSLConfig sslResource) throws IOException {
		this.port = port;
		this.proxySocket = new ServerSocket(port);
		this.sslResource = sslResource;
	}

	/**
	 * Listen to client connections
	 * @throws IOException 
	 */
	public void listen() {
		while(this.proxyOn) {
			try {
				Socket clientSocket = proxySocket.accept();
				ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, this.sslResource, this.expositionStrategy);
				Thread t = new Thread(connectionHandler);
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public ServerSocket getProxySocket() {
		return proxySocket;
	}

	public void setProxySocket(ServerSocket proxySocket) {
		this.proxySocket = proxySocket;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isProxyOn() {
		return proxyOn;
	}

	public void setProxyOn(boolean proxyOn) {
		this.proxyOn = proxyOn;
	}

	public SSLConfig getSslResource() {
		return sslResource;
	}

	public void setSslResource(SSLConfig sslResource) {
		this.sslResource = sslResource;
	}

	public ExpositionStrategy getExpositionStrategy() {
		return expositionStrategy;
	}

	public void setExpositionStrategy(ExpositionStrategy expositionStrategy) {
		this.expositionStrategy = expositionStrategy;
	}

}
