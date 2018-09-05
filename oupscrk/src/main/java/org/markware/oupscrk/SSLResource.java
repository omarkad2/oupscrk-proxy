package org.markware.oupscrk;

import java.io.File;
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
public class SSLResource {

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
	public SSLResource(String rootFolder) {
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			/* CA KEY */
			this.caKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(rootFolder + "ca.key").getFile()));
			
			/* INT KEY */
			this.intKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(rootFolder + "int.key").getFile()));
			
			/* CA CERT */
			this.caCert = SecurityUtils.loadX509Certificate(new File(classLoader.getResource(rootFolder + "ca.crt").getFile()));
			
			/* INT CERT */
			this.intCert = SecurityUtils.loadX509Certificate(new File(classLoader.getResource(rootFolder + "int.crt").getFile()));
			
			/* CERT KEY */
			this.certKey = SecurityUtils.loadPrivateKey(new File(classLoader.getResource(rootFolder + "cert.key").getFile()));
			
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
