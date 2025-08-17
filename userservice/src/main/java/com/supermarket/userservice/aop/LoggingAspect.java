package com.supermarket.userservice.aop;

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

    // a logger instance for this aspect
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Pointcut("within(com.supermarket.userservice.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.supermarket.userservice.service..*)")
    public void serviceLayer() {
    }

    @Before("controllerLayer()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("Enter: {}.{}() with argument[s] = [{}]", className, methodName, Arrays.toString(args));
    }


    @AfterReturning(pointcut = "controllerLayer()", returning = "result")
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

    @Around("serviceLayer()")
    public Object logExecutionTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        long endTime = System.currentTimeMillis();

        String className = proceedingJoinPoint.getSignature().getDeclaringTypeName();
        String methodName = proceedingJoinPoint.getSignature().getName();

        log.info("Execution time: {}.{}() executed in {} ms",
                className, methodName, (endTime - startTime));
        return result;
    }
}