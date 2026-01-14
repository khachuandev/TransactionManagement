package com.example.Transaction.util;

import com.example.Transaction.config.Translator;
import com.example.Transaction.exception.RSAKeyLoadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Slf4j
@Component
public class RSAUtils {
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String CLASSPATH_PREFIX = "classpath:";

    @Value("${rsa.keystore.path}")
    private String keystorePath;

    @Value("${rsa.keystore.password}")
    private String keystorePassword;

    @Value("${rsa.key.alias}")
    private String keyAlias;

    /* ========== LOAD KEYSTORE ========== */
    private KeyStore loadKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

            InputStream is;
            if (keystorePath.startsWith(CLASSPATH_PREFIX)) {
                String path = keystorePath.substring(CLASSPATH_PREFIX.length());
                log.info("Loading keystore from classpath: {}", path);
                ClassPathResource resource = new ClassPathResource(path);
                is = resource.getInputStream();
            } else {
                log.info("Loading keystore from file system: {}", keystorePath);
                is = new FileInputStream(keystorePath);
            }

            try (is) {
                keyStore.load(is, keystorePassword.toCharArray());
                log.info("Keystore loaded successfully");
                return keyStore;
            }
        } catch (Exception e) {
            log.error("Failed to load PKCS12 keystore from: {}", keystorePath, e);
            throw new RSAKeyLoadException(
                    Translator.toLocale("rsa.private.key.notfound", keyAlias)
            );
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            KeyStore ks = loadKeyStore();
            PrivateKey key = (PrivateKey) ks.getKey(keyAlias, keystorePassword.toCharArray());
            if (key == null) {
                throw new RSAKeyLoadException(
                        Translator.toLocale("rsa.private.key.notfound", keyAlias)
                );
            }
            return key;
        } catch (Exception e) {
            log.error("Failed to load private key", e);
            throw new RSAKeyLoadException(Translator.toLocale("rsa.private.key.failed"));
        }
    }

    public PublicKey getPublicKey() {
        try {
            KeyStore ks = loadKeyStore();
            PublicKey key = ks.getCertificate(keyAlias).getPublicKey();
            if (key == null) {
                throw new RSAKeyLoadException(Translator.toLocale("rsa.public.key.notfound", keyAlias));
            }
            log.debug("Public key loaded successfully");
            return key;
        } catch (Exception e) {
            log.error("Failed to load public key", e);
            throw new RSAKeyLoadException(Translator.toLocale("rsa.public.key.failed"), e);
        }
    }

    /* ========== SIGN & VERIFY ========== */
    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(getPrivateKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("RSA sign failed", e);
            throw new RSAKeyLoadException(Translator.toLocale("rsa.sign.failed"), e);
        }
    }

    public boolean verify(String data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(getPublicKey());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            log.error("RSA verify failed", e);
            return false;
        }
    }
}