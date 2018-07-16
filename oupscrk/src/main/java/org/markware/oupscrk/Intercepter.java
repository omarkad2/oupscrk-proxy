package org.markware.oupscrk;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main thread
 * @author citestra
 *
 */
public class Intercepter {

	/**
	 * CA resources
	 */
	private final static String CA_FOLDER = "CA/";
	
	/**
	 * is proxy running ?
	 */
	private boolean running = false;
	
	/**
	 * Proxy socket
	 */
	private ServerSocket proxySocket;
	
	/**
	 * CA key
	 */
	private String caKey;
	
	/**
	 * CA Cert
	 */
	private String caCert;
	
	/**
	 * Cert Key
	 */
	private String certKey;
	
	/**
	 * Website cert certificates
	 */
	private Path certsFolder;
	
	/**
	 * Main method
	 * @param args [0] port (optional)
	 */
	public static void main(String[] args) {
		int port = 9999;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		
		System.out.println("Proxy listening on port : " + port);
		
		Intercepter interceptor = new Intercepter(port);
		interceptor.listen();
		
		System.out.println("Proxy stopped listening on port : " + port);
	}
	
	/**
	 * Constructor
	 * @param port
	 */
	public Intercepter(int port) {
		File file;
		try {
			this.proxySocket = new ServerSocket(port);
			this.running = true;
			ClassLoader classLoader = getClass().getClassLoader();
			/* CA KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.key").getFile());
			this.caKey = new String(Files.readAllBytes(file.toPath()));
			/* CA CERT */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.crt").getFile());
			this.caCert = new String(Files.readAllBytes(file.toPath()));
			/* CERT KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "cert.key").getFile());
			this.certKey = new String(Files.readAllBytes(file.toPath()));
			/* CERTS FOLDER */
			this.certsFolder = Files.createTempDirectory(Paths.get(""), "certs");
		} catch (IOException e) {
			System.out.println("Couldn't create proxy socket");
			e.printStackTrace();
		}
	}
	
	/**
	 * Listen to client connections
	 */
	public void listen() {
		while(this.running) {
			try {
				Socket clientSocket = this.proxySocket.accept();
				Thread t = new Thread(new ConnectionHandler(clientSocket, this.caKey, this.caCert, this.certKey, this.certsFolder));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
