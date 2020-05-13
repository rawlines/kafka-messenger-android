package com.github.crypto;

import android.util.Base64;

import com.github.db.conversation.ConversationMessage.MetaData;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class Cryptography {
      private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
      private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

      private static PrivateKey priv;

      public static void setPrivKey(PrivateKey key) { priv = key; }

      public static MetaData decryptBytes(byte[] bytes) throws Exception {

            String[] tokens = new String(bytes, StandardCharsets.ISO_8859_1).split(Pattern.quote("$$"));

            byte[] saltb = Base64.decode(tokens[0], Base64.DEFAULT);
            IvParameterSpec salt = new IvParameterSpec(saltb);
            byte[] encodedAesKey = decrypt(tokens[1].getBytes(StandardCharsets.ISO_8859_1), priv, RSA_ALGORITHM);

            SecretKeySpec aesKey = new SecretKeySpec(encodedAesKey, 0, encodedAesKey.length, "AES");
            String s = new String(decrypt(tokens[2].getBytes(StandardCharsets.ISO_8859_1), aesKey, salt, AES_ALGORITHM),
                  StandardCharsets.ISO_8859_1);

            Gson gson = new Gson();

            return gson.fromJson(s, MetaData.class);
      }

      public static byte[] metadataToCryptedBytes(MetaData md, byte[] b64key) throws Exception {
            PublicKey key = getKeyFromB64(b64key);

            Gson gson = new Gson();
            String json = gson.toJson(md, MetaData.class);

            byte[] jsonb = json.getBytes(StandardCharsets.ISO_8859_1);


            KeyGenerator aesGenerator = KeyGenerator.getInstance("AES");
            aesGenerator.init(256, new SecureRandom());
            SecretKey aesKey = aesGenerator.generateKey();

            byte[] saltb = new byte[16];
            new Random().nextBytes(saltb);

            IvParameterSpec salt = new IvParameterSpec(saltb);
            byte[] aesEncryptedKey = encrypt(aesKey.getEncoded(), key, RSA_ALGORITHM);
            byte[] contentEncrypted = encrypt(jsonb, aesKey, salt, AES_ALGORITHM);

            return (new String(Base64.encode(saltb, Base64.DEFAULT), StandardCharsets.ISO_8859_1) +
                  "$$" + new String(aesEncryptedKey, StandardCharsets.ISO_8859_1) +
                  "$$" + new String(contentEncrypted, StandardCharsets.ISO_8859_1)).getBytes(StandardCharsets.ISO_8859_1);
      }


      private static PublicKey getKeyFromB64(byte[] b64Key) throws Exception {
            byte[] decodedKey = Base64.decode(b64Key, Base64.DEFAULT);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(decodedKey);
            return factory.generatePublic(encodedKeySpec);
      }

      //Encrypt
      private static byte[] encrypt(byte[] b, Key pub, String algorithm) throws Exception {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, pub);
            return Base64.encode(cipher.doFinal(b), Base64.DEFAULT);
      }

      private static byte[] encrypt(byte[] b, Key pub, IvParameterSpec salt, String algorithm) throws Exception {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, pub, salt);
            return Base64.encode(cipher.doFinal(b), Base64.DEFAULT);
      }


      //Decrypt
      private static byte[] decrypt(byte[] b, Key secret, String algorithm) throws Exception {
            byte[] decoded = Base64.decode(b, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secret);
            return cipher.doFinal(decoded);
      }

      private static byte[] decrypt(byte[] b, Key secret, IvParameterSpec salt, String algorithm) throws Exception {
            byte[] decoded = Base64.decode(b, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secret, salt);
            return cipher.doFinal(decoded);
      }
}
