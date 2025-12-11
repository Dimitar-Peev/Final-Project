package com.exam.eventhub.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.exam.eventhub..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;

            log.info("Executed {} in {} ms", methodName, duration);
            return result;

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("Exception in {} after {} ms: {}", methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}
