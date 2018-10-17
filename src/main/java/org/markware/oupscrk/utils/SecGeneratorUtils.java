package org.markware.oupscrk.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

/**
 * Generate SSL resources (CA key, CA cert & SNIs cert key)
 * @author citestra
 *
 */
public class SecGeneratorUtils {

	/**
	 * generate CA certificate
	 */
	private static X509Certificate createCACert(
			PublicKey pubKey,
			PrivateKey privKey) throws Exception {
		// signers name 
		X500NameBuilder nameBuilder = new X500NameBuilder();

		nameBuilder.addRDN(BCStyle.C, "MA");
		nameBuilder.addRDN(BCStyle.O, "Oupscrk");
		nameBuilder.addRDN(BCStyle.OU, "www.oupscrk.com");
		nameBuilder.addRDN(BCStyle.CN, "Ouspcrk-CA");

		X500Name x500Name = nameBuilder.build();
		
		// create the certificate - version 3
		X509v3CertificateBuilder v3Bldr = new JcaX509v3CertificateBuilder(x500Name, new BigInteger(32, new SecureRandom()),
				new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30) * 10),
				x500Name, pubKey);

		v3Bldr.addExtension(
				Extension.basicConstraints,
			    true, 
			    new BasicConstraints(true));
		
		X509CertificateHolder certHldr = v3Bldr.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privKey));

		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr);

		cert.checkValidity(new Date());

		cert.verify(pubKey);

		return cert;
	}


	/**
	 * Convert X509 Certificate to string
	 * @param cert
	 * @return X509 Certificate as string
	 */
	private static String certToString(X509Certificate cert) {
	    StringWriter sw = new StringWriter();
	    try {
	        sw.write("-----BEGIN CERTIFICATE-----\n");
	        sw.write(DatatypeConverter.printBase64Binary(cert.getEncoded()).replaceAll("(.{64})", "$1\n"));
	        sw.write("\n-----END CERTIFICATE-----\n");
	    } catch (CertificateEncodingException e) {
	        e.printStackTrace();
	    }
	    return sw.toString();
	}

	/**
	 * Main method
	 * @throws Exception
	 */
	public static void generate() throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        
		// set up the keys
        KeyPair caKeys = keyGen.generateKeyPair();
        KeyPair certKeys = keyGen.generateKeyPair();
        
		X509Certificate caCert = createCACert(caKeys.getPublic(), caKeys.getPrivate());

		// CA KEY
		JcaPEMWriter caPemWriter = new JcaPEMWriter(new FileWriter(new File("ca.key")));
		JcaPKCS8Generator gen1 = new JcaPKCS8Generator(caKeys.getPrivate(), null);  
		PemObject obj1 = gen1.generate();
		caPemWriter.writeObject(obj1);
		caPemWriter.close();
		
		// CA CERT
		BufferedWriter caCertWriter = new BufferedWriter(new FileWriter("ca.crt"));
		caCertWriter.write(certToString(caCert));
		caCertWriter.flush();
		caCertWriter.close();
				
		// CERT KEYS (HOST KEYS)
		JcaPEMWriter certPemWriter = new JcaPEMWriter(new FileWriter(new File("cert.key")));
		JcaPKCS8Generator gen5 = new JcaPKCS8Generator(certKeys.getPrivate(), null);  
		PemObject obj5 = gen5.generate();
		certPemWriter.writeObject(obj5);
		certPemWriter.close();

	}

}
