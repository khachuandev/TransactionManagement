package com.example.Transaction.util;

import com.example.Transaction.config.Translator;
import com.example.Transaction.exception.AESProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@Component
public class AESUtils {
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    private static final String hashAlgorithm = "SHA-256";

    @Value("${encryption.aes.master.key}")
    private String masterKey;

    /**
     * Tạo SecretKey từ master key cố định
     * Dùng SHA-256 để tạo key 256-bit từ master key string
     */
    private SecretKeySpec getSecretKey() {
        try {
            byte[] key = masterKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance(hashAlgorithm);
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32);
            return new SecretKeySpec(key, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate secret key", e);
            throw new AESProcessingException(Translator.toLocale("aes.key.failed"), e);
        }
    }

    /**
     * Mã hóa cho DB - IV được embed vào encrypted data
     * Format: IV(16 bytes) + EncryptedData
     * Dùng để mã hóa Account Number trước khi lưu DB
     */
    public String encryptForDB(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;

        try {
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            log.error("AES encryption failed", e);
            throw new AESProcessingException(Translator.toLocale("aes.encrypt.failed"), e);
        }
    }

    /**
     * Giải mã từ DB - Extract IV từ encrypted data
     * Dùng để giải mã Account Number khi đọc từ DB
     */
    public String decryptFromDB(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return encryptedBase64;

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            if (combined.length < IV_SIZE) {
                throw new IllegalArgumentException(Translator.toLocale("aes.invalid.data"));
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_SIZE, combined.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            log.error("AES decryption failed", e);
            throw new AESProcessingException(Translator.toLocale("aes.decrypt.failed"), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 input", e);
            throw new AESProcessingException(Translator.toLocale("aes.invalid.data"), e);
        }
    }
}
