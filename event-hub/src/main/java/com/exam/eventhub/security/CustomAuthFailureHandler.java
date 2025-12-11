package com.exam.eventhub.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {

        if (exception instanceof LockedException) {
            setDefaultFailureUrl("/login?error=blocked");
        } else if (exception instanceof RememberMeAuthenticationException) {
            setDefaultFailureUrl("/login?rememberExpired");
        } else if (exception instanceof CookieTheftException) {
            setDefaultFailureUrl("/login?rememberExpired");
        } else {
            setDefaultFailureUrl("/login?error");
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}