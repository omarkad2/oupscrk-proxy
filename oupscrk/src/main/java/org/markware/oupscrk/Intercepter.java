package org.markware.oupscrk;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

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
		File file;
		try {
			this.proxySocket = new ServerSocket(port);
			this.running = true;
			ClassLoader classLoader = getClass().getClassLoader();
			/* CA KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.key").getFile());
			this.caKey = loadPrivateKey(new String(Files.readAllBytes(file.toPath())));
			
			/* Int KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "int.key").getFile());
			this.intKey = loadPrivateKey(new String(Files.readAllBytes(file.toPath())));
			
			/* CA CERT */
			file = new File(classLoader.getResource(CA_FOLDER + "ca.crt").getFile());
			String caCertStr = new String(Files.readAllBytes(file.toPath()));
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			this.caCert = (X509Certificate) cf.generateCertificate(
					new ByteArrayInputStream(Base64.getDecoder().decode(
							caCertStr.replace("-----BEGIN CERTIFICATE-----", "")
									 .replace("-----END CERTIFICATE-----", "")
									 .replaceAll("\\n",  "")
									)));
			
			/* INT CERT */
			file = new File(classLoader.getResource(CA_FOLDER + "int.crt").getFile());
			String intCertStr = new String(Files.readAllBytes(file.toPath()));
			this.intCert = (X509Certificate) cf.generateCertificate(
					new ByteArrayInputStream(Base64.getDecoder().decode(
							intCertStr.replace("-----BEGIN CERTIFICATE-----", "")
									 .replace("-----END CERTIFICATE-----", "")
									 .replaceAll("\\n",  "")
									)));
			
			/* CERT KEY */
			file = new File(classLoader.getResource(CA_FOLDER + "cert.key").getFile());
			this.certKey = loadPrivateKey(new String(Files.readAllBytes(file.toPath())));
			/* CERTS FOLDER */
			this.certsFolder = Files.exists(Paths.get("certs")) ? Paths.get("certs") : Files.createDirectory(Paths.get("certs"));
		} catch (IOException | GeneralSecurityException e) {
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
				Thread t = new Thread(new ConnectionHandler(clientSocket, this.caKey, this.intKey, this.caCert, this.intCert, this.certKey, this.certsFolder));
				t.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Key loadPublicKey(String stored) throws GeneralSecurityException, IOException {
		byte[] data = Base64.getDecoder().decode((stored.getBytes()));
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		return fact.generatePublic(spec);

	}


	public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException, IOException {
		byte[] clear = Base64.getDecoder().decode(
				key64.replace("-----BEGIN PRIVATE KEY-----", "")
					 .replace("-----END PRIVATE KEY-----", "")
					 .replaceAll("\\n",  "")
					 .getBytes());
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		PrivateKey priv = fact.generatePrivate(keySpec);
		Arrays.fill(clear, (byte) 0);
		return priv;

	}

}
