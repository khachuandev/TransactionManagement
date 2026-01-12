package com.example.Transaction.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SensitiveDataMasker {
    // ======= Các giá trị mặc định =======
    private static final String NULL_VALUE = "null";
    private static final String EMPTY_VALUE = "";
    private static final int MAX_MASK_LENGTH = 10;
    private static final char MASK_CHAR = '?';
    private static final String EXCEPTION_DEFAULT = "Exception occurred";
    private static final String MASK_PATTERN = "[0-9A-Za-z]";

    /**
     * Che dữ liệu nhạy cảm bằng dấu ?
     */
    public String mask(Object data) {
        if (data == null) return NULL_VALUE;
        String str = data.toString();
        if (str.isEmpty()) return EMPTY_VALUE;
        int length = Math.min(str.length(), MAX_MASK_LENGTH);
        return String.valueOf(MASK_CHAR).repeat(length);
    }

    /**
     * Che exception message
     */
    public String maskException(Exception e) {
        if (e == null || e.getMessage() == null) return EXCEPTION_DEFAULT;
        return e.getMessage().replaceAll(MASK_PATTERN, String.valueOf(MASK_CHAR));
    }

    /**
     * Log an toàn với dữ liệu nhạy cảm được che
     */
    public void logSafely(String message, Object... sensitiveData) {
        if (sensitiveData == null || sensitiveData.length == 0) {
            log.info(message);
            return;
        }

        Object[] masked = new Object[sensitiveData.length];
        for (int i = 0; i < sensitiveData.length; i++) {
            masked[i] = mask(sensitiveData[i]);
        }
        log.info(message, masked);
    }
}
