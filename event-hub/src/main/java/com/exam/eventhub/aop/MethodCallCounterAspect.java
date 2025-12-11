package com.exam.eventhub.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Aspect
@Component
public class MethodCallCounterAspect {

    private final ConcurrentHashMap<String, AtomicInteger> methodCalls = new ConcurrentHashMap<>();

    @AfterReturning("execution(* com.exam.eventhub..service..*(..))")
    public void countCalls(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();

        methodCalls.putIfAbsent(method, new AtomicInteger(0));
        int count = methodCalls.get(method).incrementAndGet();

        if (count % 10 == 0) {
            log.info("Method {} was called {} times", method, count);
        }
    }
}
