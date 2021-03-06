package org.markware.oupscrk.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

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
import org.markware.oupscrk.config.SSLConfig;

public class SecurityUtils {

	/**
	 * Create cert for hostname using my CA
	 * @param hostname
	 * @param certFile
	 * @param sslResource
	 * @throws Exception
	 */
	public static void createHostCert(
			String hostname,
			String certFile,
			SSLConfig sslResource) throws Exception {

		Security.addProvider(new BouncyCastleProvider());

		// Get public key from private key
		RSAPrivateCrtKey privk = (RSAPrivateCrtKey)sslResource.getCertKey();
		RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey certPublicKey = keyFactory.generatePublic(publicKeySpec);

		// subject name builder.
		X500NameBuilder nameBuilder = new X500NameBuilder();

		nameBuilder.addRDN(BCStyle.CN, hostname);
		
		// create the certificate - version 3
		X509v3CertificateBuilder v3Bldr = new JcaX509v3CertificateBuilder(sslResource.getCaCert(), new BigInteger(32, new SecureRandom()),
				new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30), new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 30)),
				nameBuilder.build(), certPublicKey);

		// extensions
		JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

		v3Bldr.addExtension(
				Extension.subjectKeyIdentifier,
				false,
				extUtils.createSubjectKeyIdentifier(certPublicKey));

		v3Bldr.addExtension(
				Extension.authorityKeyIdentifier,
				false,
				extUtils.createAuthorityKeyIdentifier(sslResource.getCaCert()));

		ASN1Encodable[] subjectAlternativeNames = new ASN1Encodable[]
			    {
			        new GeneralName(GeneralName.dNSName, hostname)
			    };
		
		v3Bldr.addExtension(
				Extension.subjectAlternativeName, 
				false, 
				new DERSequence(subjectAlternativeNames));

		X509CertificateHolder certHldr = v3Bldr.build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(sslResource.getCaKey()));

		X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHldr);

		cert.checkValidity(new Date());

		cert.verify(sslResource.getCaCert().getPublicKey());

		Certificate[] chain = new Certificate[2];
		chain[1] = (Certificate) sslResource.getCaCert();
		chain[0] = (Certificate) cert;

		KeyStore store = KeyStore.getInstance("PKCS12", "BC");
		store.load(null, null);
		store.setKeyEntry(hostname, sslResource.getCertKey(), null, chain);
		try (FileOutputStream fOut = new FileOutputStream(certFile)) {
			store.store(fOut, "secret".toCharArray());
			fOut.close();
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	/**
	 * 
	 * @param pubKeyFile public key file
	 * @return Public key
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static Key loadPublicKey(File pubKeyFile) throws GeneralSecurityException, IOException {
		String pubKeyStr = new String(Files.readAllBytes(pubKeyFile.toPath()));
		byte[] data = Base64.getDecoder().decode((pubKeyStr.getBytes()));
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		return fact.generatePublic(spec);
	}

	/**
	 * Load Private key from file
	 * @param privKeyFile private key File
	 * @return private key
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static PrivateKey loadPrivateKey(File privKeyFile) throws GeneralSecurityException, IOException {
		String privKeyStr = new String(Files.readAllBytes(privKeyFile.toPath()));
		byte[] clear = Base64.getDecoder().decode(
				privKeyStr.replace("-----BEGIN PRIVATE KEY-----", "")
						  .replace("-----END PRIVATE KEY-----", "")
						  .replaceAll("\\n",  "")
						  .getBytes());
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		PrivateKey priv = fact.generatePrivate(keySpec);
		Arrays.fill(clear, (byte) 0);
		return priv;
	}
	
	/**
	 * Load X509 Certificate from file
	 * @param certFile certificate file
	 * @return X509Certificate
	 * @throws CertificateException
	 * @throws IOException
	 */
	public static X509Certificate loadX509Certificate(File certFile) throws CertificateException, IOException {
		String certStr = new String(Files.readAllBytes(certFile.toPath()));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return (X509Certificate) cf.generateCertificate(
				new ByteArrayInputStream(Base64.getDecoder().decode(
											certStr.replace("-----BEGIN CERTIFICATE-----", "")
												   .replace("-----END CERTIFICATE-----", "")
												   .replaceAll("\\n",  "")
				)));
	}

	/**
	 * Load Private key from Inputstream
	 * @param resourceAsStream resource stream
	 * @return private key
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public static PrivateKey loadPrivateKey(InputStream resourceAsStream) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String privKeyStr = new BufferedReader(new InputStreamReader(resourceAsStream))
				  .lines().collect(Collectors.joining("\n"));
		byte[] clear = Base64.getDecoder().decode(
				privKeyStr.replace("-----BEGIN PRIVATE KEY-----", "")
						  .replace("-----END PRIVATE KEY-----", "")
						  .replaceAll("\\n",  "")
						  .getBytes());
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		PrivateKey priv = fact.generatePrivate(keySpec);
		Arrays.fill(clear, (byte) 0);
		return priv;
	}

	/**
	 * Load X509 Certificate from Inputstream
	 * @param resourceAsStream resource stream
	 * @return X509Certificate
	 * @throws CertificateException
	 */
	public static X509Certificate loadX509Certificate(InputStream resourceAsStream) throws CertificateException {
		String certStr = new BufferedReader(new InputStreamReader(resourceAsStream))
				  .lines().collect(Collectors.joining("\n"));
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		return (X509Certificate) cf.generateCertificate(
				new ByteArrayInputStream(Base64.getDecoder().decode(
											certStr.replace("-----BEGIN CERTIFICATE-----", "")
												   .replace("-----END CERTIFICATE-----", "")
												   .replaceAll("\\n",  "")
				)));
	}
}
