package org.markware.oupscrk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.markware.oupscrk.utils.SecurityUtils;

/**
 * SSL resource loader
 * @author omarkad
 *
 */
public class SSLConfig {

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
	 * Constructor
	 * @param port
	 */
	public SSLConfig(String rootFolder) {
		try {
			/* CA KEY */
			this.caKey = SecurityUtils.loadPrivateKey(SSLConfig.class.getResourceAsStream(rootFolder + "ca.key"));
			
			/* INT KEY */
			this.intKey = SecurityUtils.loadPrivateKey(SSLConfig.class.getResourceAsStream(rootFolder + "int.key"));
			
			/* CA CERT */
			this.caCert = SecurityUtils.loadX509Certificate(SSLConfig.class.getResourceAsStream(rootFolder + "ca.crt"));
			
			/* INT CERT */
			this.intCert = SecurityUtils.loadX509Certificate(SSLConfig.class.getResourceAsStream(rootFolder + "int.crt"));
			
			/* CERT KEY */
			this.certKey = SecurityUtils.loadPrivateKey(SSLConfig.class.getResourceAsStream(rootFolder + "cert.key"));
			
			/* CERTS FOLDER */
			this.certsFolder = Files.exists(Paths.get("certs")) ? Paths.get("certs") : Files.createDirectory(Paths.get("certs"));
			
		} catch (IOException | GeneralSecurityException e) {
			System.out.println("Couldn't load Keys/Certificate from filesystem");
			e.printStackTrace();
		}
	}
	
	public boolean isAllSet() {
		return this.caKey != null && this.caCert != null && this.certKey != null && this.certsFolder != null;
	}
	
	public PrivateKey getCaKey() {
		return caKey;
	}

	public PrivateKey getIntKey() {
		return intKey;
	}

	public X509Certificate getCaCert() {
		return caCert;
	}

	public X509Certificate getIntCert() {
		return intCert;
	}

	public PrivateKey getCertKey() {
		return certKey;
	}

	public Path getCertsFolder() {
		return certsFolder;
	}
}
