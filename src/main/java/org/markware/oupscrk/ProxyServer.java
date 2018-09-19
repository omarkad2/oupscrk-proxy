package org.markware.oupscrk;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.markware.oupscrk.ui.impl.LogFileExpositionStrategy;

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

	public ProxyServer(int port, SSLConfig sslResource) throws IOException {
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
				Thread t = new Thread(new ConnectionHandler(clientSocket, this.sslResource, new LogFileExpositionStrategy()));
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

}
