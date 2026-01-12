package com.example.Transaction.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig {

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            new Locale("en"), new Locale("vi")
    );

    @Bean
    public LocaleResolver localeResolver() {
        return new AcceptHeaderLocaleResolver() {
            @Override
            public Locale resolveLocale(@NonNull HttpServletRequest request) {
                String header = request.getHeader("Accept-Language");
                if (!StringUtils.hasText(header)) {
                    return new Locale("en");
                }
                Locale matched = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED_LOCALES);
                return matched != null ? matched : new Locale("en");
            }
        };
    }

    @Bean
    public ResourceBundleMessageSource bundleMessageSource(){
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }
}
