package org.markware.oupscrk;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class SecurityUtils {

	public static void createHostCert(
			String hostname,
			String certFile,
			PrivateKey certPrivateKey,
			PrivateKey caPrivateKey,
			PrivateKey intPrivateKey,
			X509Certificate caCert,
			X509Certificate intCert) throws Exception {

		Security.addProvider(new BouncyCastleProvider());

		// Get public key from private key
		RSAPrivateCrtKey privk = (RSAPrivateCrtKey)certPrivateKey;
		RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey certPublicKey = keyFactory.generatePublic(publicKeySpec);

		//
		// subject name builder.
		//
		X500NameBuilder nameBuilder = new X500NameBuilder();

		nameBuilder.addRDN(BCStyle.CN, hostname);
		
		//
		// create the certificate - version 3
		//
		X509v3CertificateBuilder v3Bldr = new JcaX509v3CertificateBuilder(caCert, new BigInteger(32, new SecureRandom()),
				new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
				nameBuilder.build(), certPublicKey);

		//
		// extensions
		//
		JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

		v3Bldr.addExtension(
				Extension.subjectKeyIdentifier,
				false,
				extUtils.createSubjectKeyIdentifier(certPublicKey));

		v3Bldr.addExtension(
				Extension.authorityKeyIdentifier,
				false,
				extUtils.createAuthorityKeyIdentifier(caCert));

		ASN1Encodable[] subjectAlternativeNames = new ASN1Encodable[]
			    {
			        new GeneralName(GeneralName.dNSName, hostname)
			    };
		
		v3Bldr.addExtension(
				Extension.subjectAlternativeName, 
				false, 
				new DERSequence(subjectAlternativeNames));

//		v3Bldr.addExtension(
//				Extension.basicConstraints,
//				true,
//				new BasicConstraints(0));

		X509CertificateHolder certHldr = v3Bldr.build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(caPrivateKey));

		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr);

		cert.checkValidity(new Date());

		cert.verify(caCert.getPublicKey());

		Certificate[] chain = new Certificate[2];
		chain[1] = (Certificate) caCert;
//		chain[1] = (Certificate) intCert;
		chain[0] = (Certificate) cert;

		KeyStore store = KeyStore.getInstance("PKCS12", "BC");
		store.load(null, null);
		store.setKeyEntry(hostname, certPrivateKey, null, chain);
		FileOutputStream fOut = new FileOutputStream(certFile);
		store.store(fOut, "secret".toCharArray());
		fOut.close();
	}
}
