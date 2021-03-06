package de.gematik.rbellogger.util;

import java.security.*;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CryptoUtils {

    public static final int GCM_IV_LENGTH_IN_BYTES = 12;
    public static final int GCM_TAG_LENGTH_IN_BYTES = 16;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static byte[] ecka(PrivateKey prk, PublicKey puk)
        throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
        byte[] sharedSecret;
        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(prk);
        ka.doPhase(puk, true);
        sharedSecret = ka.generateSecret();
        return sharedSecret;
    }

    public static byte[] hkdf(byte[] ikm, String info, int lengthInBytes)
        throws IllegalArgumentException, DataLengthException {
        return hkdf(ikm, info.getBytes(), lengthInBytes);
    }

    public static byte[] hkdf(byte[] ikm, byte[] info, int lengthInBytes)
        throws IllegalArgumentException, DataLengthException {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm, null, info));
        byte[] okm = new byte[lengthInBytes];
        hkdf.generateBytes(okm, 0, lengthInBytes);
        return okm;
    }

    public static Optional<byte[]> decrypt(byte[] encMessage, Key secretKey) {
        return decrypt(encMessage, secretKey, GCM_IV_LENGTH_IN_BYTES, GCM_TAG_LENGTH_IN_BYTES);
    }

    public static Optional<byte[]> decrypt(byte[] encMessage, Key secretKey, int gcmIvLengthInBytes,
        int gcmTagLengthInBytes) {
        try {
            byte[] iv = Arrays.copyOfRange(encMessage, 0, gcmIvLengthInBytes);
            byte[] cipherText = Arrays.copyOfRange(encMessage, GCM_IV_LENGTH_IN_BYTES, encMessage.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");//NOSONAR

            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(gcmTagLengthInBytes * 8, iv));

            return Optional.ofNullable(cipher.doFinal(cipherText));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}