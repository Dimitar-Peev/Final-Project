package com.exam.eventhub.config;

import com.exam.eventhub.security.LoginPageInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@AllArgsConstructor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginPageInterceptor loginPageInterceptor;
    private final LocaleChangeInterceptor localeChangeInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor);

        registry.addInterceptor(loginPageInterceptor)
                .addPathPatterns("/login", "/register")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**");
    }
}