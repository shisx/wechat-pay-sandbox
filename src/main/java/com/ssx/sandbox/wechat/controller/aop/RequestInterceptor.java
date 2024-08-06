package com.ssx.sandbox.wechat.controller.aop;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RequestInterceptor implements HandlerInterceptor {

    /**
     * 以 controller 包下定义的所有请求为切入点
     */
    @Pointcut(value = "execution(public * com.ssx.sandbox.wechat.controller..*.*(..))")
    public void controllerLog() {
    }

    /**
     * 在切点之前织入
     *
     * @param joinPoint
     * @throws Throwable
     */
    @Before("controllerLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 打印请求 url
        log.info("Request URL    : {}", request.getRequestURL().toString());
        // 打印 Http method
        log.info("HTTP Method    : {}", request.getMethod());
        // 打印调用 controller 的全路径以及执行方法
        // log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        // 打印请求的 IP
        log.info("Request IP     : {}", request.getRemoteAddr());
        // 打印请求入参
        if ("post".equalsIgnoreCase(request.getMethod()) && request.getContentType() != null) {
            if (request.getContentType().toLowerCase().contains("json")) {
                log.info("Request Body   : {}", JSON.toJSONString(joinPoint.getArgs()[0]));
            } else if (request.getContentType().toLowerCase().contains("xml")) {
                log.info("Request Body   : {}", joinPoint.getArgs()[0]);
            } else {
                log.info("Request Body   : {}", Arrays.toString(joinPoint.getArgs()));
            }
        } else {
            log.info("Request Args   : {}", Arrays.toString(joinPoint.getArgs()));
        }
    }

    /**
     * 在切点之后织入
     *
     * @throws Throwable
     */
    @After("controllerLog()")
    public void doAfter() throws Throwable {
    }

    /**
     * 环绕
     *
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("controllerLog()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        log.info("========================================== Start ==========================================");
        long startTime = System.currentTimeMillis();
        Object result = proceedingJoinPoint.proceed();
        // 打印出参
        log.info("Response Args  : {}", result);
        // 执行耗时
        log.info("Time-Consuming : {} ms", System.currentTimeMillis() - startTime);
        log.info("=========================================== End ===========================================");
        return result;
    }


}
