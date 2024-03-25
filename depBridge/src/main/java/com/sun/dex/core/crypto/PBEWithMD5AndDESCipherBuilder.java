package com.sun.dex.core.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PBEWithMD5AndDESCipherBuilder extends BaseCipherBuilder {

    @Override
    public void init(String cipherName, String keySeed, boolean isEnc) {
        super.init(cipherName, keySeed, isEnc);

        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(keySeed.toCharArray());
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWITHMD5andDES");
            key1Obj = factory.generateSecret(pbeKeySpec);
            key1 = key1Obj.getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        key2 = keyGen.nextBytes(8);
        key2Obj = new PBEParameterSpec(key2, 100);
    }
}
