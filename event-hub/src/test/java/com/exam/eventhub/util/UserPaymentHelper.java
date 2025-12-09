package com.exam.eventhub.util;

import com.exam.eventhub.payment.client.dto.PaymentResponse;
import com.exam.eventhub.user.model.Role;
import com.exam.eventhub.user.model.User;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class UserPaymentHelper {

    public static User createUser(UUID id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user.setBlocked(false);
        user.setNotificationsEnabled(true);
        return user;
    }

    public static PaymentResponse createPaymentResponse(UUID paymentId, UUID userId, BigDecimal amount,
                                                        String status, LocalDateTime createdOn) {
        PaymentResponse payment = new PaymentResponse();
        payment.setPaymentId(paymentId);
        payment.setBookingId(UUID.randomUUID());
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setMessage("Payment processed successfully");
        payment.setCreatedOn(createdOn);
        return payment;
    }
}
