package com.example.Transaction.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

@Component
public class Translator {
    private static ResourceBundleMessageSource messageSource;

    @Autowired
    private Translator(ResourceBundleMessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String toLocale(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public static String toLocale(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
