package org.markware.oupscrk;

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
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;

public class SecGenerator {

	/**
	 * we generate the CA's certificate
	 */
	public static X509Certificate createMasterCert(
			PublicKey       pubKey,
			PrivateKey      privKey)
					throws Exception {
		//
		// signers name 
		//
		X500NameBuilder nameBuilder = new X500NameBuilder();

		nameBuilder.addRDN(BCStyle.C, "MA");
		nameBuilder.addRDN(BCStyle.O, "Markware");
		nameBuilder.addRDN(BCStyle.OU, "www.markware.com");
		nameBuilder.addRDN(BCStyle.CN, "Markware-CA");

		X500Name x500Name = nameBuilder.build();
		//
		// create the certificate - version 3
		//
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
	 * we generate an intermediate certificate signed by our CA
	 */
	public static X509Certificate createIntermediateCert(
			PublicKey       pubKey,
			PrivateKey      caPrivKey,
			X509Certificate caCert)
					throws Exception {
		//
		// subject name builder.
		//
		X500NameBuilder nameBuilder = new X500NameBuilder();

		nameBuilder.addRDN(BCStyle.C, "MA");
		nameBuilder.addRDN(BCStyle.O, "Intermediate");
		nameBuilder.addRDN(BCStyle.OU, "Intermediate Certificate");
		nameBuilder.addRDN(BCStyle.CN, "Intermediate");
		
		//
		// create the certificate - version 3
		//
		X509v3CertificateBuilder v3Bldr = new JcaX509v3CertificateBuilder(caCert, new BigInteger(32, new SecureRandom()),
				new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30) * 10),
				nameBuilder.build(), pubKey);

		//
		// extensions
		//
		JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

		v3Bldr.addExtension(
				Extension.subjectKeyIdentifier,
				false,
				extUtils.createSubjectKeyIdentifier(pubKey));

		v3Bldr.addExtension(
				Extension.authorityKeyIdentifier,
				false,
				extUtils.createAuthorityKeyIdentifier(caCert));

		v3Bldr.addExtension(
				Extension.basicConstraints,
				false,
				new BasicConstraints(0));

		X509CertificateHolder certHldr = v3Bldr.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caPrivKey));

		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr);

		cert.checkValidity(new Date());

		cert.verify(caCert.getPublicKey());

		return cert;
	}

	public static String certToString(X509Certificate cert) {
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

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        
		//
		// set up the keys
		//
        KeyPair          caKeys = keyGen.generateKeyPair();
        KeyPair          intKeys = keyGen.generateKeyPair();
        KeyPair          certKeys = keyGen.generateKeyPair();
        
		X509Certificate caCert = createMasterCert(caKeys.getPublic(), caKeys.getPrivate());
		X509Certificate intCert = createIntermediateCert(intKeys.getPublic(), caKeys.getPrivate(), caCert);

		// CA KEYS
		JcaPEMWriter writer1 = new JcaPEMWriter(new FileWriter(new File("ca.key")));
		JcaPKCS8Generator gen1 = new JcaPKCS8Generator(caKeys.getPrivate(), null);  
		PemObject obj1 = gen1.generate();
		writer1.writeObject(obj1);
		writer1.close();

		JcaPEMWriter writer2 = new JcaPEMWriter(new FileWriter(new File("ca_pub.key")));
		writer2.writeObject(caKeys.getPublic());
		writer2.close();

		// INTERMEDIATE KEYS
		JcaPEMWriter writer3 = new JcaPEMWriter(new FileWriter(new File("int.key")));
		JcaPKCS8Generator gen3 = new JcaPKCS8Generator(intKeys.getPrivate(), null);  
		PemObject obj3 = gen3.generate();
		writer3.writeObject(obj3);
		writer3.close();

		JcaPEMWriter writer4 = new JcaPEMWriter(new FileWriter(new File("int_pub.key")));
		writer4.writeObject(intKeys.getPublic());
		writer4.close();

		// CERT KEYS
		JcaPEMWriter writer5 = new JcaPEMWriter(new FileWriter(new File("cert.key")));
		JcaPKCS8Generator gen5 = new JcaPKCS8Generator(certKeys.getPrivate(), null);  
		PemObject obj5 = gen5.generate();
		writer5.writeObject(obj5);
		writer5.close();

		JcaPEMWriter writer6 = new JcaPEMWriter(new FileWriter(new File("cert_pub.key")));
		writer6.writeObject(certKeys.getPublic());
		writer6.close();
		
		// CA CERT
		BufferedWriter bWriter1 = new BufferedWriter(new FileWriter("ca.crt"));
		bWriter1.write(certToString(caCert));
		bWriter1.flush();
		bWriter1.close();
		
		// INT CERT
		BufferedWriter bWriter2 = new BufferedWriter(new FileWriter("int.crt"));
		bWriter2.write(certToString(intCert));
		bWriter2.flush();
		bWriter2.close();
	}

}
