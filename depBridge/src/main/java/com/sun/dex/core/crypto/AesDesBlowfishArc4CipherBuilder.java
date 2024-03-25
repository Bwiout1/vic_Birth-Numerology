package com.sun.dex.core.crypto;


import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesDesBlowfishArc4CipherBuilder extends BaseCipherBuilder {

    @Override
    public void init(String cipherName, String keySeed, boolean isEnc) {
        super.init(cipherName, keySeed, isEnc);

        if(cipherName.startsWith("AES") || cipherName.startsWith("AES_128") || cipherName.startsWith("AES_256")){
            algName = "AES";
            if(cipherName.startsWith("AES_128") || cipherName.startsWith("AES/") || cipherName.equals("AES")){
                key1 = keyGen.nextBytes(16);
            }else if(cipherName.startsWith("AES_256")) {
                key1 = keyGen.nextBytes(32);
            }

            if(cipherName.contains("CBC") || cipherName.contains("CTR") || cipherName.contains("CTS")
                || cipherName.contains("CFB") || cipherName.contains("OFB")){
                //need IV
                key2 = keyGen.nextBytes(16);
                key2Obj = new IvParameterSpec(key2);
            } else if(cipherName.contains("GCM")){
                //need IV
                key2 = keyGen.nextBytes(12);
                key2Obj = new GCMParameterSpec(128, key2);
            }
        }else if(cipherName.equals("BLOWFISH")){
//            int key1Len = 32 + keyGen.nextInt(32); //[32~64]
            key1 = keyGen.nextBytes(32);//theory:32~448
        }else if(cipherName.equals("DES")){
            key1 = keyGen.nextBytes(8);
        }else if(cipherName.equals("DESEDE")){
            key1 = keyGen.nextBytes(24);
        }else if(cipherName.startsWith("ARC4")){
            key1 = keyGen.nextBytes(40);
        }else{
            key1 = keyGen.nextBytes(32);
        }


        if(null != key1) {
            key1Obj = new SecretKeySpec(key1, algName);
        }

    }
}
