package org.markware.oupscrk;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

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
	 * Cert Key
	 */
	private PrivateKey certKey;

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
		try {
			this.proxySocket = new ServerSocket(port);
			this.running = true;
			ClassLoader classLoader = getClass().getClassLoader();
			/* CA KEY */
			this.caKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(CA_FOLDER + "ca.key").getFile()));
			
			/* INT KEY */
			this.intKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(CA_FOLDER + "int.key").getFile()));
			
			/* CA CERT */
			this.caCert = SecurityUtils.loadX509Certificate(new File(classLoader.getResource(CA_FOLDER + "ca.crt").getFile()));
			
			/* INT CERT */
			this.intCert = SecurityUtils.loadX509Certificate(new File(classLoader.getResource(CA_FOLDER + "int.crt").getFile()));
			
			/* CERT KEY */
			this.certKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(CA_FOLDER + "cert.key").getFile()));
			
			/* CERTS FOLDER */
			this.certsFolder = Files.exists(Paths.get("certs")) ? Paths.get("certs") : Files.createDirectory(Paths.get("certs"));
			
		} catch (IOException | GeneralSecurityException e) {
			System.out.println("Couldn't load Keys/Certificate from filesystem");
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
				Thread t = new Thread(new ConnectionHandler(clientSocket, this.caKey, this.intKey, this.caCert, this.intCert, this.certKey, this.certsFolder));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
