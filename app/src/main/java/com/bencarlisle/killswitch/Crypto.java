package com.bencarlisle.killswitch;

import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

class Crypto {

    private KeyPair keyPair;

    Crypto(DataControl dataControl, String name, boolean needsKey) {
        if (needsKey) {
            generateKeyPair(dataControl, name);
        } else {
            initKeyPair(dataControl, name);
        }
    }

    private void initKeyPair(DataControl dataControl, String name) {
        try {
            byte[] privateKeyBytes = dataControl.getPrivateKey(name);
            byte[] publicKeyBytes = dataControl.getPublicKey(name);

            KeyFactory keyFactory= KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey= keyFactory.generatePrivate(pkcs8EncodedKeySpec);

            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);

            this.keyPair = new KeyPair(publicKey, privateKey);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void writeKeysToFile(DataControl dataControl, String name) {
        dataControl.setKeys(name, keyPair.getPrivate().getEncoded(), keyPair.getPublic().getEncoded());
    }

    private void generateKeyPair(DataControl dataControl, String name) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            this.keyPair = kpg.generateKeyPair();
            writeKeysToFile(dataControl, name);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    byte[] getPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    @SuppressWarnings("SameReturnValue")
    int getChallengeSize() {
        return 256;
    }

    byte[] decryptChallenge(byte[] challenge) {
        try {
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
            rsa.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return rsa.doFinal(challenge);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

}
