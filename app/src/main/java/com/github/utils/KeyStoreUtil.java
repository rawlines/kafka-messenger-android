package com.github.utils;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

public abstract class KeyStoreUtil {
      private static final String DEFAULT_USER = "kafkaUser";
      public static final String DEFAULT_ALIAS = "default";

      /**
       * Gets an existing keystore from localStorage, if not exists, a new one is returned
       *
       * @param password - Password of keystore
       */
      public static KeyStore getKeyStore(File keyStoreFile, String password) throws Exception {
            KeyStore keyStore = KeyStore.getInstance("BKS");

            if (keyStoreFile.exists()) {
                  FileInputStream fis = new FileInputStream(keyStoreFile);
                  keyStore.load(fis, password.toCharArray());
                  fis.close();
            } else {
                  keyStore.load(null, password.toCharArray());
                  KeyPair keyPair = generateKeyPair(2048);
                  X509Certificate cert = generateCertificate(keyPair, 365, DEFAULT_USER);
                  keyStore.setKeyEntry(DEFAULT_ALIAS, keyPair.getPrivate(), password.toCharArray(), new X509Certificate[]{cert});

                  keyStoreFile.createNewFile();
                  FileOutputStream fos = new FileOutputStream(keyStoreFile);
                  keyStore.store(fos, password.toCharArray());
                  fos.close();
            }

            return keyStore;
      }

      private static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            return keyPairGenerator.generateKeyPair();
      }

      private static X509Certificate generateCertificate(KeyPair keyPair, int days, String tag) throws OperatorCreationException, CertificateException, IOException {

            Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
            Date endDate = new Date(System.currentTimeMillis() + days * 24 * 60 * 60 * 1000);

            X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            nameBuilder.addRDN(BCStyle.O, tag);
            nameBuilder.addRDN(BCStyle.OU, tag);
            nameBuilder.addRDN(BCStyle.L, tag);

            X500Name x500Name = nameBuilder.build();
            Random random = new Random();

            SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            X509v1CertificateBuilder v1CertGen = new X509v1CertificateBuilder(x500Name
                  , BigInteger.valueOf(random.nextLong())
                  , startDate
                  , endDate
                  , x500Name
                  , subjectPublicKeyInfo);

            ContentSigner sigGen;
            Security.addProvider(new BouncyCastleProvider());
            sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("SC").build(keyPair.getPrivate());
            X509CertificateHolder x509CertificateHolder = v1CertGen.build(sigGen);

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(x509CertificateHolder.getEncoded());
            return (X509Certificate) certificateFactory.generateCertificate(in);
      }
}
