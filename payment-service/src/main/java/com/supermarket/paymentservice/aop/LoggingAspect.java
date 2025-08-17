package com.supermarket.paymentservice.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut("within(com.supermarket.paymentservice.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.supermarket.paymentservice.service..*)")
    public void serviceLayer() {
    }

    @Before("controllerLayer() || serviceLayer()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String argsString = Arrays.toString(args);
        log.info("Enter: {}.{}() with argument[s] = [{}]", className, methodName, argsString);
    }

    @AfterReturning(pointcut = "controllerLayer() || serviceLayer()", returning = "result")
    public void logMethodExitSuccess(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();


        log.info("Exit: {}.{}() with result = [{}]", className, methodName, result);
    }

    @AfterThrowing(pointcut = "controllerLayer() || serviceLayer()", throwing = "exception")
    public void logMethodExitError(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.error("Exception in {}.{}() | exception = '{}'", className, methodName, exception.getMessage());
    }

    // --- AROUND ADVICE (Execution Time) ---
    @Around("serviceLayer()")
    public Object logExecutionTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed(); // Execute the target method
        long endTime = System.currentTimeMillis();

        String className = proceedingJoinPoint.getSignature().getDeclaringTypeName();
        String methodName = proceedingJoinPoint.getSignature().getName();

        log.info("Execution time: {}.{}() executed in {} ms", className, methodName, (endTime - startTime));
        return result;
    }

}


