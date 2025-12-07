package com.exam.app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class PaymentGateway {

    public boolean charge(UUID userId, BigDecimal amount) {
        log.info("Simulating payment charge for user {} amount {}", userId, amount);
        return true;
    }
}