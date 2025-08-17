package com.supermarket.cartservice.aop;


import com.supermarket.cartservice.model.Cart;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.supermarket.cartservice.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.supermarket.cartservice.service..*)")
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
        String resultString = result != null ? safeObjectToString(result) : "null";

        log.info("Exit: {}.{}() with result = [{}]", className, methodName, resultString);
    }

    @AfterThrowing(pointcut = "controllerLayer() || serviceLayer()", throwing = "exception")
    public void logMethodExitError(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.error("Exception in {}.{}() | exception = '{}'", className, methodName, exception.getMessage());
    }

    // --- AROUND ADVICE (Execution Time) ---
    @Around("serviceLayer()") // Apply timing only to service layer
    public Object logExecutionTime(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed(); // Execute the target method
        long endTime = System.currentTimeMillis();

        String className = proceedingJoinPoint.getSignature().getDeclaringTypeName();
        String methodName = proceedingJoinPoint.getSignature().getName();

        log.info("Execution time: {}.{}() executed in {} ms", className, methodName, (endTime - startTime));
        return result;
    }

    private String safeObjectToString(Object obj) {
        if (obj == null) return "null";

        // Handle Collections/Arrays to avoid huge logs
        if (obj instanceof java.util.Collection) {
            String elements = ((java.util.Collection<?>) obj).stream()
                    .map(this::safeObjectToString) // Recursive call
                    .limit(5) // Show first 5 items max
                    .collect(Collectors.joining(", "));

            if (((java.util.Collection<?>) obj).size() > 5) elements += ", ...";
            return "[" + elements + "] (Size: " + ((java.util.Collection<?>) obj).size() + ")";
        }
        if (obj.getClass().isArray()) {
            String elements = Arrays.stream((Object[]) obj)
                    .map(this::safeObjectToString)
                    .limit(5)
                    .collect(Collectors.joining(", "));
            if (((Object[]) obj).length > 5) elements += ", ...";
            return "[" + elements + "] (Size: " + ((Object[]) obj).length + ")";
        }

        String objStr = obj.toString();
        // Limit overall length
        if (objStr.length() > 500) return objStr.substring(0, 497) + "...";
        return objStr;
    }
}
